package com.example;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;

public class MessageGenerator {

    public Message<String> generateMsg() {
        return MessageBuilder.withPayload("Hello World").build();
    }

}
