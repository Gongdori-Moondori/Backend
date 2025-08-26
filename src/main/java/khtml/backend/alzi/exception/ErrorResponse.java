package khtml.backend.alzi.exception;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
    private String code;
    private String message;
    private int status;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String path;
    private List<FieldError> errors;

    @Getter
    @Builder
    public static class FieldError {
        private String field;
        private String value;
        private String reason;
    }

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatus().value())
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, String path, List<FieldError> errors) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatus().value())
                .timestamp(LocalDateTime.now())
                .path(path)
                .errors(errors)
                .build();
    }
}
