package khtml.backend.alzi.config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import khtml.backend.alzi.exception.ErrorCode;
import khtml.backend.alzi.utils.ApiResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증은 되었지만 권한이 없는 사용자가 접근할 때 처리하는 클래스
 */
@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
		AccessDeniedException accessDeniedException) throws IOException, ServletException {

		log.warn("Access denied to: {} - {}", request.getRequestURI(), accessDeniedException.getMessage());

		// 응답 설정
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		// 에러 응답 생성
		ApiResponse<Void> errorResponse = ApiResponse.failure(
			ErrorCode.ACCESS_DENIED.getMessage(),
			ErrorCode.ACCESS_DENIED.getCode()
		);

		// JSON으로 응답
		String jsonResponse = objectMapper.writeValueAsString(errorResponse);
		response.getWriter().write(jsonResponse);
	}
}
