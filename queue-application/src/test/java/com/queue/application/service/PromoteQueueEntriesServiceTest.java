package com.queue.application.service;

import com.queue.application.config.QueuePromotionProperties;
import com.queue.application.dto.PromoteCommand;
import com.queue.application.dto.PromoteResult;
import com.queue.application.port.out.QueueCommandPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PromoteQueueEntriesServiceTest {

    private QueueCommandPort queueCommandPort;
    private QueuePromotionProperties properties;
    private PromoteQueueEntriesService service;

    @BeforeEach
    void setUp() {
        queueCommandPort = mock(QueueCommandPort.class);

        properties = new QueuePromotionProperties();
        properties.setQueueId("default");
        properties.setBatchSize(25);
        properties.setMaxActiveCount(100);
        properties.setActiveTtlSeconds(180L);

        service = new PromoteQueueEntriesService(queueCommandPort, properties);
    }

    @Test
    @DisplayName("설정값으로 PromoteCommand 를 생성해 out port 로 전달한다")
    void promote_buildsCommandAndDelegates() {
        when(queueCommandPort.promoteWaitingEntries(any(PromoteCommand.class)))
                .thenReturn(new PromoteResult("default", 25, 7));

        PromoteResult result = service.promote();

        ArgumentCaptor<PromoteCommand> captor = ArgumentCaptor.forClass(PromoteCommand.class);
        verify(queueCommandPort).promoteWaitingEntries(captor.capture());

        PromoteCommand actual = captor.getValue();

        assertThat(actual.queueId()).isEqualTo("default");
        assertThat(actual.maxActiveCount()).isEqualTo(100);
        assertThat(actual.promoteBatchSize()).isEqualTo(25);
        assertThat(actual.activeTtl()).isEqualTo(Duration.ofSeconds(180));

        assertThat(result.queueId()).isEqualTo("default");
        assertThat(result.requestedCount()).isEqualTo(25);
        assertThat(result.promotedCount()).isEqualTo(7);
    }
}