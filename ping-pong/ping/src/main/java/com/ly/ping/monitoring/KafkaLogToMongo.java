package com.ly.ping.monitoring;

import com.ly.commons.constants.CommonConstants;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.*;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Component
@Log4j2
public class KafkaLogToMongo {
    @Value("${server.port}")
    private String serverPort;
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${spring.kafka.consumer.key-deserializer}")
    private String keyDeserializer;
    @Value("${spring.kafka.consumer.value-deserializer}")
    private String valueDeserializer;
    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;
    @Value("${spring.data.mongodb.uri}")
    private String mongodbUri;
    @Value("${spring.data.mongodb.database}")
    private String mongodbDatabase;
    @Value("${spring.data.mongodb.username}")
    private String mongodbUsername;
    @Value("${spring.data.mongodb.password}")
    private String mongodbPassword;
    @Value("${spring.data.mongodb.host}")
    private String mongodbHost;
    @Value("${spring.data.mongodb.port}")
    private int mongodbPort;

    private KafkaConsumer<String, String> consumer;
    private MongoCollection<Document> collection;

    @PostConstruct
    public void init() throws UnsupportedEncodingException {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-mongo-group");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        // 手动提交offset
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singleton(CommonConstants.DEFAULT_TOPIC_PREFIX + serverPort));

        ConnectionString connectionString = new ConnectionString(mongodbUri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        MongoClient mongoClient = MongoClients.create(settings);
        MongoDatabase database = mongoClient.getDatabase(mongodbDatabase);
        collection = database.getCollection(CommonConstants.DEFAULT_DOCUMENT);
    }

    @Scheduled(fixedDelay = 10000)
    public void pollKafkaAndInsertToMongo() {
        try {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            List<Document> documents = new ArrayList<>();

            for (ConsumerRecord<String, String> record : records) {
                JSONObject json = new JSONObject(record.value());
                Document doc = Document.parse(json.toString());
                documents.add(doc);

                if (documents.size() == CommonConstants.MONGODB_BATCH_SUBMIT_MUM) {
                    collection.insertMany(documents);
                    consumer.commitSync();
                    documents.clear();
                }
            }

            if (!documents.isEmpty()) {
                collection.insertMany(documents);
                consumer.commitSync();
            }
            documents.clear();
        } catch (CommitFailedException e) {
            log.error("kafka提交失败：" + e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
