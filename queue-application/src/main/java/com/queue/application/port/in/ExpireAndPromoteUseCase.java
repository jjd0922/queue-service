package com.queue.application.port.in;

import com.queue.application.dto.ExpireAndPromoteCommand;
import com.queue.application.dto.ExpireAndPromoteResult;

public interface ExpireAndPromoteUseCase {
    ExpireAndPromoteResult execute(ExpireAndPromoteCommand command);
}
