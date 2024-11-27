package com.ly.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitingConfig {
    @Bean
    public KeyResolver pathKeyResolver() {
        // 基于api控制
        return exchange -> Mono.just(exchange.getRequest().getPath().toString());
    }
}
