package com.queue.api.dto;

import com.queue.application.dto.command.EnterQueueCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EnterQueueRequest(
        @NotBlank String queueId,
        @NotNull @Positive Long userId
) {
    public EnterQueueCommand toCommand() {
        return new EnterQueueCommand(queueId, userId);
    }
}
