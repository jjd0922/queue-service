package com.queue.api.controller;

import com.queue.api.dto.EnterQueueRequest;
import com.queue.api.dto.EnterQueueResponse;
import com.queue.application.dto.result.EnterQueueResult;
import com.queue.application.port.in.EnterQueueUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/queues")
public class QueueController {

    private final EnterQueueUseCase enterQueueUseCase;

    @PostMapping("/enter")
    public EnterQueueResponse enter(@Valid @RequestBody EnterQueueRequest request) {
        EnterQueueResult result = enterQueueUseCase.enter(request.toCommand());
        return EnterQueueResponse.from(result);
    }
}
