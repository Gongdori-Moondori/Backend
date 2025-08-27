package khtml.backend.alzi.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 커스텀 예외 처리
	 */
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse> handleCustomException(CustomException e, WebRequest request) {
		log.error("CustomException: {}", e.getMessage(), e);
		ErrorResponse response = ErrorResponse.of(e.getErrorCode(), request.getDescription(false));
		return new ResponseEntity<>(response, e.getErrorCode().getStatus());
	}

	/**
	 * 인증 예외 처리
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e, WebRequest request) {
		log.error("AuthenticationException: {}", e.getMessage(), e);
		ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_REQUEST, request.getDescription(false));
		return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
	}

	/**
	 * 잘못된 자격 증명 예외 처리
	 */
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e, WebRequest request) {
		log.error("BadCredentialsException: {}", e.getMessage(), e);
		ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_REQUEST, request.getDescription(false));
		return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
	}

	/**
	 * 접근 거부 예외 처리
	 */
	@ExceptionHandler({AccessDeniedException.class, java.nio.file.AccessDeniedException.class})
	public ResponseEntity<ErrorResponse> handleAccessDeniedException(Exception e, WebRequest request) {
		log.error("AccessDeniedException: {}", e.getMessage(), e);
		ErrorResponse response = ErrorResponse.of(ErrorCode.ACCESS_DENIED, request.getDescription(false));
		return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
	}

	/**
	 * Validation 예외 처리 (@Valid, @Validated)
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
		WebRequest request) {
		log.error("MethodArgumentNotValidException: {}", e.getMessage(), e);

		List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> ErrorResponse.FieldError.builder()
				.field(error.getField())
				.value(error.getRejectedValue() != null ? error.getRejectedValue().toString() : "")
				.reason(error.getDefaultMessage())
				.build())
			.collect(Collectors.toList());

		ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT, request.getDescription(false), fieldErrors);
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * 바인딩 예외 처리
	 */
	@ExceptionHandler(BindException.class)
	public ResponseEntity<ErrorResponse> handleBindException(BindException e, WebRequest request) {
		log.error("BindException: {}", e.getMessage(), e);

		List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> ErrorResponse.FieldError.builder()
				.field(error.getField())
				.value(error.getRejectedValue() != null ? error.getRejectedValue().toString() : "")
				.reason(error.getDefaultMessage())
				.build())
			.collect(Collectors.toList());

		ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT, request.getDescription(false), fieldErrors);
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * 타입 불일치 예외 처리
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
		MethodArgumentTypeMismatchException e, WebRequest request) {
		log.error("MethodArgumentTypeMismatchException: {}", e.getMessage(), e);
		ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT, request.getDescription(false));
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * 필수 파라미터 누락 예외 처리
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
		MissingServletRequestParameterException e, WebRequest request) {
		log.error("MissingServletRequestParameterException: {}", e.getMessage(), e);
		ErrorResponse response = ErrorResponse.of(ErrorCode.MISSING_REQUIRED_FIELD, request.getDescription(false));
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * HTTP 메소드 지원하지 않음 예외 처리
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
		HttpRequestMethodNotSupportedException e, WebRequest request) {
		log.error("HttpRequestMethodNotSupportedException: {}", e.getMessage(), e);
		ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_REQUEST, request.getDescription(false));
		return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
	}

	/**
	 * 파일 업로드 크기 초과 예외 처리
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e,
		WebRequest request) {
		log.error("MaxUploadSizeExceededException: {}", e.getMessage(), e);
		ErrorResponse response = ErrorResponse.of(ErrorCode.FILE_SIZE_EXCEEDED, request.getDescription(false));
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * 데이터 무결성 위반 예외 처리
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e,
		WebRequest request) {
		log.error("DataIntegrityViolationException: {}", e.getMessage(), e);
		ErrorResponse response = ErrorResponse.of(ErrorCode.DATA_INTEGRITY_VIOLATION, request.getDescription(false));
		return new ResponseEntity<>(response, HttpStatus.CONFLICT);
	}

	/**
	 * 일반적인 예외 처리
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e, WebRequest request) {
		// favicon.ico 요청 오류는 로그에서 제외
		String requestUri = request.getDescription(false);
		if (requestUri != null && requestUri.contains("favicon.ico")) {
			// 파비콘 요청은 로그 레벨을 DEBUG로 낮춰서 출력량 감소
			log.debug("Favicon request ignored: {}", e.getMessage());
		} else {
			log.error("Exception: {}", e.getMessage(), e);
		}
		
		ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getDescription(false));
		return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
