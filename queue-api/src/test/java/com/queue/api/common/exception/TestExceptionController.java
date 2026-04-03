package com.queue.api.common.exception;

import com.queue.application.common.exception.BaseException;
import com.queue.application.common.exception.CommonErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@Validated
class TestExceptionController {

    @GetMapping("/base-exception")
    public void baseException() {
        throw new BaseException(CommonErrorCode.NOT_FOUND);
    }

    @GetMapping("/constraint")
    public void constraint(@RequestParam @Min(1) long id) {
    }

    @PostMapping("/validation")
    public void validation(@Valid @RequestBody TestRequest request) {
    }

    static class TestRequest {

        @NotBlank(message = "name은 필수입니다.")
        private String name;

        @Min(value = 1, message = "count는 1 이상이어야 합니다.")
        private Integer count;

        public String getName() {
            return name;
        }

        public Integer getCount() {
            return count;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }
}