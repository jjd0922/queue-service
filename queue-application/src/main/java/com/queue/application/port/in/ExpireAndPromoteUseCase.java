package com.queue.application.port.in;

import com.queue.application.dto.command.ExpireAndPromoteCommand;
import com.queue.application.dto.result.ExpireAndPromoteResult;

public interface ExpireAndPromoteUseCase {
    ExpireAndPromoteResult execute(ExpireAndPromoteCommand command);
}
