package com.example;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;

public class MessageGenerator {

    public Message<String> generateMsg() {
        return MessageBuilder.withPayload("Hello World").build();
    }

}
