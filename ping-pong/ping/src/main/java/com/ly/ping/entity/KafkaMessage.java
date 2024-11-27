package com.ly.ping.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class KafkaMessage {
    private String topic;
    private String msg;
}
