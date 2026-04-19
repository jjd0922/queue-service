package com.queue.infrastructure.queue.worker;

import com.queue.application.dto.PromoteResult;
import com.queue.application.port.in.PromoteQueueEntriesUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class QueuePromotionWorkerTest {

    private PromoteQueueEntriesUseCase promoteQueueEntriesUseCase;
    private QueuePromotionWorker worker;

    @BeforeEach
    void setUp() {
        promoteQueueEntriesUseCase = mock(PromoteQueueEntriesUseCase.class);
        worker = new QueuePromotionWorker(promoteQueueEntriesUseCase);
    }

    @Test
    @DisplayName("스케줄 실행 시 승격 유스케이스를 호출한다")
    void promote_callsUseCase() {
        when(promoteQueueEntriesUseCase.promote())
                .thenReturn(new PromoteResult("default", 10, 3));

        worker.promote();

        verify(promoteQueueEntriesUseCase).promote();
    }
}