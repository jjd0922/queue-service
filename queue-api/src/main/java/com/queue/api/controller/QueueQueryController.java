package com.queue.api.controller;

import com.queue.api.dto.QueueStatusResponse;
import com.queue.application.dto.query.GetQueueStatusQuery;
import com.queue.application.port.in.GetQueueStatusUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/queues/{queueName}/entries")
public class QueueQueryController {

    private final GetQueueStatusUseCase getQueueStatusUseCase;

    @GetMapping("/{queueToken}")
    public QueueStatusResponse getQueueStatus(
            @PathVariable String queueName,
            @PathVariable String queueToken
    ) {
        return QueueStatusResponse.from(
                getQueueStatusUseCase.getQueueStatus(
                        new GetQueueStatusQuery(queueName, queueToken)
                )
        );
    }
}

