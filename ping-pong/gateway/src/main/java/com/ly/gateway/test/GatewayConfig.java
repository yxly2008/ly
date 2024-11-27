package com.ly.gateway.test;

import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Log4j2
public class GatewayConfig {
    @Bean
    public GlobalFilter logRequestTimeFilter() {
        return (exchange, chain) -> {
            // 预留入口，此处可以对请求做额外处理
            return chain.filter(exchange);
        };
    }
}
