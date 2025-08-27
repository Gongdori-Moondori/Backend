package khtml.backend.alzi.jwt;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import khtml.backend.alzi.exception.ErrorCode;
import khtml.backend.alzi.exception.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {
		
		log.warn("Unauthorized access attempt: {} {}", request.getMethod(), request.getRequestURI());
		log.debug("Authentication exception: {}", authException.getMessage());

		// 토큰이 없는 경우와 유효하지 않은 경우를 구분
		String authorization = request.getHeader("Authorization");
		ErrorCode errorCode;
		
		if (authorization == null || authorization.trim().isEmpty()) {
			errorCode = ErrorCode.INVALID_JWT_TOKEN;
			log.warn("No Authorization header provided");
		} else if (!authorization.startsWith("Bearer ")) {
			errorCode = ErrorCode.INVALID_JWT_TOKEN;
			log.warn("Invalid Authorization header format: {}", authorization.substring(0, Math.min(authorization.length(), 10)) + "...");
		} else {
			errorCode = ErrorCode.INVALID_JWT_TOKEN;
			log.warn("Invalid or expired token");
		}

		response.setStatus(errorCode.getStatus().value());
		response.setContentType("application/json;charset=UTF-8");
		
		ErrorResponse errorResponse = ErrorResponse.of(errorCode, request.getRequestURI());
		String jsonResponse = objectMapper.writeValueAsString(errorResponse);
		
		response.getWriter().write(jsonResponse);
	}
}
