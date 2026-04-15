package com.queue.application.dto;

public record ExpireActiveTokensCommand(int batchSize) {

    public ExpireActiveTokensCommand {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }
    }
}
