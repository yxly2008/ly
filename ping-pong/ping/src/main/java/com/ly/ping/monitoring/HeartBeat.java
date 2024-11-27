package com.ly.ping.monitoring;

import com.ly.commons.constants.CommonConstants;
import com.ly.commons.filelock.CommonFileLock;
import com.ly.ping.utils.KafkaLogUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;


@Component
@Import(CommonFileLock.class)
@Log4j2
public class HeartBeat implements CommandLineRunner {
    private final static String gatewayUrl = "http://localhost:8080";
    private WebClient webClient;

    /**
     * 使用规划的端口做分区字段
     */
    @Value("${server.port}")
    private int kafkaPartition;

    /**
     * 当前应用使用的topic
     */
    private String kafka_topic = null;

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private CommonFileLock commonFileLock;
    @Autowired
    private KafkaLogUtil kafkaLogUtil;

    @Autowired
    public HeartBeat(@Value("${server.port}") int serverPort) {
        this.webClient = WebClient.builder().baseUrl(gatewayUrl).build();
        this.kafka_topic = CommonConstants.DEFAULT_TOPIC_PREFIX + serverPort;
    }

    @Override
    public void run(String... args) {
        Flux.interval(Duration.ofMillis(CommonConstants.HEART_BEAT_INTERVAL)).flatMap(ping -> {
            Boolean limitLock = commonFileLock.getRateLimitLock();
            if (limitLock == null) {
                kafkaTemplate.executeInTransaction(operations -> {
                    operations.send(kafka_topic,
                            kafkaPartition,
                            kafkaLogUtil.formatLog(kafka_topic,
                                    CommonConstants.LOG_RATE_LIMITED));
                    return true;
                });
            } else if (limitLock) {
                // 符合请求发送条件
                return sentPing(webClient).onErrorResume(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException webClientError = (WebClientResponseException) error;
                        if (webClientError.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                            kafkaTemplate.executeInTransaction(operations -> {
                                operations.send(kafka_topic,
                                        kafkaPartition,
                                        kafkaLogUtil.formatLog(kafka_topic, CommonConstants.LOG_SENT_AND_PONG_THROTTLED));
                                return true;
                            });
                        } else {
                            // 服务端响应code非429
                            log.error("心跳发送失败" + error.getMessage());
                        }
                    } else {
                        // 服务端未响应
                        log.error("心跳发送失败" + error.getMessage());
                    }
                    // 继续流
                    return Mono.empty();
                });
            } else {
                kafkaTemplate.executeInTransaction(operations -> {
                    operations.send(kafka_topic,
                            kafkaPartition,
                            kafkaLogUtil.formatLog(kafka_topic, CommonConstants.LOG_SENT_AND_PONG_THROTTLED));
                    return true;
                });
            }
            // 继续流
            return Mono.empty();
        }).subscribe(response -> {
            log.info("接收到心跳返回信息：" + response);
            kafkaTemplate.executeInTransaction(operations -> {
                operations.send(kafka_topic,
                        kafkaPartition,
                        kafkaLogUtil.formatLog(kafka_topic, CommonConstants.LOG_SENT_AND_PONG));
                return true;
            });
        }, error -> {
            log.error("心跳发送失败" + error.getMessage());
        });

        //Thread.currentThread().join();
    }

    private Mono<String> sentPing(WebClient webClient) {
        return webClient.post().uri("/pong").bodyValue("Hello").retrieve().
                bodyToMono(String.class).doOnSubscribe(subscription -> {
                    log.info("开始发送心跳请求...");
                }).doOnError(error -> {
                    log.error("心跳发送失败：" + error.getMessage());
                }).doOnSuccess(success -> {
                    log.info("心跳发送成功");
                    kafkaTemplate.executeInTransaction(operations -> {
                        operations.send(kafka_topic,
                                kafkaPartition,
                                kafkaLogUtil.formatLog(kafka_topic,
                                        CommonConstants.LOG_SENT_AND_PONG));
                        return true;
                    });
                });
    }
}
