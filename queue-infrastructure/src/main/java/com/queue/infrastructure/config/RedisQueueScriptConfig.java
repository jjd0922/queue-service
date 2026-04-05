package com.queue.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@Configuration
public class RedisQueueScriptConfig {

    @Bean
    public RedisScript<List> enqueueOrGetExistingScript() {
        return RedisScript.of(
                new ClassPathResource("scripts/queue/enqueue-or-get-existing.lua"),
                List.class
        );
    }
}
