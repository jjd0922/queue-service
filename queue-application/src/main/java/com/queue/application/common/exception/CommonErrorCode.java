package com.queue.application.common.exception;

public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT(400, "COMMON-400", "잘못된 요청입니다."),
    METHOD_NOT_ALLOWED(405, "COMMON-405", "지원하지 않는 HTTP 메서드입니다."),
    NOT_FOUND(404, "COMMON-404", "요청한 리소스를 찾을 수 없습니다."),
    UNAUTHORIZED(401, "COMMON-401", "인증이 필요합니다."),
    FORBIDDEN(403, "COMMON-403", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(500, "COMMON-500", "서버 내부 오류가 발생했습니다."),
    INVALID_JSON(400, "COMMON-400-JSON", "요청 본문 형식이 올바르지 않습니다."),
    INVALID_TYPE(400, "COMMON-400-TYPE", "요청 파라미터 타입이 올바르지 않습니다.");

    private final int status;
    private final String code;
    private final String message;

    CommonErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}