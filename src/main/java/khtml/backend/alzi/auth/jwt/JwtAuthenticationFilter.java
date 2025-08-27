package khtml.backend.alzi.auth.jwt;

import java.io.IOException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import khtml.backend.alzi.exception.CustomException;
import khtml.backend.alzi.exception.ErrorCode;
import khtml.backend.alzi.exception.ErrorResponse;
import khtml.backend.alzi.auth.user.CustomUserDetails;
import khtml.backend.alzi.auth.user.CustomUserDetailsService;
import khtml.backend.alzi.auth.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider tokenProvider;
	private final CustomUserDetailsService customUserDetailsService;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws IOException, ServletException {

		try {
			// refresh 엔드포인트는 JWT 검증을 건너뛰고 바로 다음 필터로 진행
			if (isRefreshTokenEndpoint(request)) {
				log.debug("Skipping JWT validation for refresh token endpoint");
				filterChain.doFilter(request, response);
				return;
			}

			String jwt = getJwtFromRequest(request);

			if (jwt != null && !jwt.trim().isEmpty()) {
				try {
					// 토큰 유효성 검사
					if (tokenProvider.validateToken(jwt)) {
						System.out.println("jwt = " + jwt);
						String userId = tokenProvider.getUserIdFromJWT(jwt);
						User user = customUserDetailsService.findByUserId(userId);
						if (user == null) {
							log.warn("User not found for userId: {}", userId);
							handleAuthenticationError(response, ErrorCode.USER_NOT_FOUND, request.getRequestURI());
							return;
						}

						UserDetails userDetails = new CustomUserDetails(user);
						JwtAuthenticationToken authentication = new JwtAuthenticationToken(userDetails, null,
							userDetails.getAuthorities());
						authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authentication);

						log.debug("Successfully authenticated user: {}", userId);
					}
				} catch (CustomException e) {
					log.error("JWT Authentication failed: {}", e.getMessage());
					handleAuthenticationError(response, e.getErrorCode(), request.getRequestURI());
					return;
				}
			}
			// 토큰이 없으면 익명 사용자로 처리 (필터 체인 계속 진행)

			filterChain.doFilter(request, response);

		} catch (Exception e) {
			log.error("Unexpected error during JWT authentication: {}", e.getMessage(), e);
			handleAuthenticationError(response, ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
		}
	}

	/**
	 * refresh token 엔드포인트인지 확인
	 */
	private boolean isRefreshTokenEndpoint(HttpServletRequest request) {
		String requestURI = request.getRequestURI();
		String method = request.getMethod();
		return "POST".equals(method) && "/api/v1/auth/refresh".equals(requestURI);
	}

	private String getJwtFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}

	private void handleAuthenticationError(HttpServletResponse response, ErrorCode errorCode, String path)
		throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setContentType("application/json;charset=UTF-8");

		ErrorResponse errorResponse = ErrorResponse.of(errorCode, path);
		String jsonResponse = objectMapper.writeValueAsString(errorResponse);

		response.getWriter().write(jsonResponse);
	}
}
