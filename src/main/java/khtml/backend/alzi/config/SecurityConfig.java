package khtml.backend.alzi.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import khtml.backend.alzi.exception.ErrorCode;
import khtml.backend.alzi.exception.ErrorResponse;
import khtml.backend.alzi.jwt.JwtAuthenticationEntryPoint;
import khtml.backend.alzi.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationEntryPoint unauthorizedHandler;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public SecurityConfig(JwtAuthenticationEntryPoint unauthorizedHandler,
		JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.unauthorizedHandler = unauthorizedHandler;
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))  // CORS 설정 추가
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(
				sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exceptionHandling ->
				exceptionHandling
					.authenticationEntryPoint(unauthorizedHandler)
					.accessDeniedHandler(accessDeniedHandler())
			)
			.authorizeHttpRequests(authorizeRequests -> authorizeRequests
				.requestMatchers("/api/**").permitAll()
				.anyRequest().authenticated()
			);

		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public AccessDeniedHandler accessDeniedHandler() {
		return (HttpServletRequest request, HttpServletResponse response,
			org.springframework.security.access.AccessDeniedException accessDeniedException) -> {

			response.setStatus(ErrorCode.ACCESS_DENIED.getStatus().value());
			response.setContentType("application/json;charset=UTF-8");

			ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.ACCESS_DENIED, request.getRequestURI());
			String jsonResponse = objectMapper.writeValueAsString(errorResponse);

			response.getWriter().write(jsonResponse);
		};
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws
		Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		// 개발환경에서는 모든 origin 허용, 운영환경에서는 구체적인 도메인 지정 필요
		configuration.setAllowedOriginPatterns(List.of("*"));  // allowedOrigins 대신 allowedOriginPatterns 사용
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
		configuration.setAllowCredentials(true);  // JWT 토큰 사용시 필요
		configuration.setMaxAge(3600L);  // preflight 요청 캐시 시간

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
