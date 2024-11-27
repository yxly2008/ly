package com.ly.ping.utils;

import com.ly.ping.entity.KafkaMessage;
import org.springframework.stereotype.Component;

@Component
public class KafkaLogUtil {
    public KafkaMessage formatLog(String topic, String msg) {
        KafkaMessage message = new KafkaMessage();
        message.setTopic(topic);
        message.setMsg(msg);
        return message;
    }
}
