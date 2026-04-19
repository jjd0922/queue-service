package com.queue.application.port.out;

import com.queue.domain.event.QueueLifecycleEvent;

public interface QueueLifecycleEventPort {
    void publish(QueueLifecycleEvent event);
}
