package com.queue.infrastructure.queue.redis.generator;

import com.queue.application.port.out.QueueTokenGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidQueueTokenGenerator implements QueueTokenGenerator {

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}