package com.queue.api.common.exception;

import com.queue.api.common.trace.TraceIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestExceptionController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("BaseException 발생 시 공통 에러 응답을 반환한다")
    void handleBaseException() throws Exception {
        mockMvc.perform(get("/test/base-exception"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-404"))
                .andExpect(jsonPath("$.error.message").value("요청한 리소스를 찾을 수 없습니다."))
                .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    @DisplayName("Validation 실패 시 필드 에러 정보를 반환한다")
    void handleMethodArgumentNotValidException() throws Exception {
        String body = """
                {
                  "name": "",
                  "count": 0
                }
                """;

        mockMvc.perform(post("/test/validation")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-400"))
                .andExpect(jsonPath("$.error.errors").isArray());
    }

    @Test
    @DisplayName("ConstraintViolation 실패 시 공통 에러 응답을 반환한다")
    void handleConstraintViolationException() throws Exception {
        mockMvc.perform(get("/test/constraint?id=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-400"));
    }
}