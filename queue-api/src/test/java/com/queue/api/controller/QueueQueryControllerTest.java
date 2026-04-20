package com.queue.api.controller;

import com.queue.application.dto.query.GetQueueStatusQuery;
import com.queue.application.dto.result.QueueStatusResult;
import com.queue.application.port.in.GetQueueStatusUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueueQueryController.class)
class QueueQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetQueueStatusUseCase getQueueStatusUseCase;

    @Nested
    @DisplayName("GET /api/v1/queues/{queueName}/entries/{queueToken}")
    class GetQueueStatus {

        @Test
        @DisplayName("대기 중인 토큰 조회에 성공하면 200과 대기 정보를 반환한다")
        void returnsWaitingResponse() throws Exception {
            // given
            String queueName = "concert-queue";
            String token = "token-1";
            Instant enteredAt = Instant.parse("2026-04-06T10:00:00Z");

            QueueStatusResult result = new QueueStatusResult(
                    queueName,
                    token,
                    "WAITING",
                    3L,
                    2L,
                    enteredAt,
                    null,
                    null
            );

            given(getQueueStatusUseCase.getQueueStatus(any(GetQueueStatusQuery.class)))
                    .willReturn(result);

            // when & then
            mockMvc.perform(get("/api/v1/queues/{queueName}/entries/{queueToken}", queueName, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.queueName").value(queueName))
                    .andExpect(jsonPath("$.queueToken").value(token))
                    .andExpect(jsonPath("$.status").value("WAITING"))
                    .andExpect(jsonPath("$.position").value(3))
                    .andExpect(jsonPath("$.aheadCount").value(2))
                    .andExpect(jsonPath("$.enteredAt").value(enteredAt.toString()));
        }

        @Test
        @DisplayName("활성 상태 토큰 조회에 성공하면 200과 활성 정보를 반환한다")
        void returnsActiveResponse() throws Exception {
            // given
            String queueName = "concert-queue";
            String token = "token-2";
            Instant enteredAt = Instant.parse("2026-04-06T10:00:00Z");
            Instant activatedAt = Instant.parse("2026-04-06T10:05:00Z");
            Instant expiresAt = Instant.parse("2026-04-06T10:15:00Z");

            QueueStatusResult result = new QueueStatusResult(
                    queueName,
                    token,
                    "ACTIVE",
                    null,
                    0L,
                    enteredAt,
                    activatedAt,
                    expiresAt
            );

            given(getQueueStatusUseCase.getQueueStatus(any(GetQueueStatusQuery.class)))
                    .willReturn(result);

            // when & then
            mockMvc.perform(get("/api/v1/queues/{queueName}/entries/{queueToken}", queueName, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.queueName").value(queueName))
                    .andExpect(jsonPath("$.queueToken").value(token))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.aheadCount").value(0))
                    .andExpect(jsonPath("$.enteredAt").value(enteredAt.toString()))
                    .andExpect(jsonPath("$.activatedAt").value(activatedAt.toString()))
                    .andExpect(jsonPath("$.expiresAt").value(expiresAt.toString()));
        }
    }
}