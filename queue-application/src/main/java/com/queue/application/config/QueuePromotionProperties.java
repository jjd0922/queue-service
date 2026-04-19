package com.queue.application.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "queue.worker.promotion")
public class QueuePromotionProperties {

    /**
     * 현재 단일 대기열 기준.
     * 멀티 queue 로 확장되면 worker 분리 또는 queue 목록 순회로 확장 가능.
     */
    private String queueId = "default";

    /** 한 번의 worker 실행에서 승격 시도할 최대 인원 */
    private int batchSize = 50;

    /** active queue 최대 수용 인원 */
    private int maxActiveCount = 100;

    /** active 상태 유지 시간(초) */
    private long activeTtlSeconds = 180L;
}