package com.queue.application.port.in;

import com.queue.application.dto.ExpireActiveTokensCommand;
import com.queue.application.dto.ExpireActiveTokensResult;

public interface ExpireActiveTokensUseCase {
    ExpireActiveTokensResult expire(ExpireActiveTokensCommand command);
}