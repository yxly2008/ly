package com.ly.pong.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
public class HeartBeatController {
    @PostMapping("/pong")
    public Mono<String> greet(ServerWebExchange exchange) {
        return Mono.just("World")
                .doOnSuccess(message -> {

                });
    }
}
