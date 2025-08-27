package khtml.backend.alzi.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
	// 공통 에러
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 내부 오류가 발생했습니다."),
	INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_002", "잘못된 입력값입니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_003", "허용되지 않은 HTTP 메서드입니다."),
	INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "COMMON_004", "잘못된 타입의 값입니다."),
	HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON_005", "접근이 거부되었습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_006", "잘못된 요청입니다."),

	FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE001", "파일 업로드에 실패했습니다."),
	INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "FILE002", "지원하지 않는 파일 형식입니다."),
	FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE003", "파일 크기가 초과되었습니다."),
	FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE004", "파일을 찾을 수 없습니다."),

	// 인증/인가 에러
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_002", "권한이 없습니다."),
	INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 JWT 토큰입니다."),
	MISSING_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH003", "토큰이 누락되었습니다."),
	EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "만료된 JWT 토큰입니다."),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_005", "유효하지 않은 Refresh 토큰입니다."),

	// 사용자 에러
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
	USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_002", "이미 존재하는 사용자입니다."),
	INVALID_USER_TYPE(HttpStatus.BAD_REQUEST, "USER_003", "잘못된 사용자 타입입니다."),

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
	SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "GEN002", "서비스를 사용할 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	ErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
}
