package khtml.backend.alzi.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // Authentication & Authorization Errors
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH002", "만료된 토큰입니다."),
    MISSING_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH003", "토큰이 누락되었습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH004", "접근 권한이 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH005", "아이디 또는 비밀번호가 잘못되었습니다."),
    
    // User Errors
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_USER_ID(HttpStatus.CONFLICT, "USER002", "이미 존재하는 아이디입니다."),
    INVALID_USER_DATA(HttpStatus.BAD_REQUEST, "USER003", "잘못된 사용자 정보입니다."),
    
    // Post/Board Errors
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST001", "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "POST002", "댓글을 찾을 수 없습니다."),
    UNAUTHORIZED_POST_ACCESS(HttpStatus.FORBIDDEN, "POST003", "게시글에 대한 권한이 없습니다."),
    UNAUTHORIZED_COMMENT_ACCESS(HttpStatus.FORBIDDEN, "POST004", "댓글에 대한 권한이 없습니다."),
    
    // Vote Errors
    VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "VOTE001", "투표를 찾을 수 없습니다."),
    ALREADY_VOTED(HttpStatus.CONFLICT, "VOTE002", "이미 투표에 참여했습니다."),
    VOTE_CLOSED(HttpStatus.BAD_REQUEST, "VOTE003", "마감된 투표입니다."),
    INVALID_VOTE_CANDIDATE(HttpStatus.BAD_REQUEST, "VOTE004", "유효하지 않은 후보입니다."),
    
    // File Errors
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE001", "파일 업로드에 실패했습니다."),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "FILE002", "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE003", "파일 크기가 초과되었습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE004", "파일을 찾을 수 없습니다."),
    
    // External API Errors
    EXTERNAL_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "API001", "외부 API 호출에 실패했습니다."),
    EXTERNAL_API_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "API002", "외부 API 응답 시간이 초과되었습니다."),
    
    // Validation Errors
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "VALID001", "입력값이 유효하지 않습니다."),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "VALID002", "필수 입력값이 누락되었습니다."),
    
    // Database Errors
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DB001", "데이터베이스 오류가 발생했습니다."),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "DB002", "데이터 무결성 제약 조건을 위반했습니다."),
    
    // General Errors
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GEN001", "서버 내부 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "GEN002", "서비스를 사용할 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "GEN003", "잘못된 요청입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
