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
import khtml.backend.alzi.jwt.OAuth2AuthenticationFailureHandler;
import khtml.backend.alzi.jwt.OAuth2AuthenticationSuccessHandler;
import khtml.backend.alzi.jwt.user.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
	private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
	private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
	private final CustomAccessDeniedHandler customAccessDeniedHandler;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))  // CORS 설정 추가
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/h2-console/**") // H2 콘솔은 CSRF 비활성화
				.disable()
			)
			.headers(headers -> headers
				.frameOptions(frameOptions -> frameOptions
					.sameOrigin() // H2 콘솔을 위해 프레임 허용
				)
			)
			.sessionManagement(
				sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorizeRequests -> authorizeRequests
				.requestMatchers("/", "/error", "/login", "/health").permitAll()
				.requestMatchers("/favicon.ico", "/css/**", "/js/**", "/images/**", "/static/**").permitAll() // 정적 리소스
				.requestMatchers("/h2-console/**").permitAll() // H2 데이터베이스 콘솔
				.requestMatchers("/oauth2/**", "/login/**").permitAll()
				.requestMatchers("/api/auth/**").permitAll() // 인증 관련 API
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger/**").permitAll()
				.requestMatchers("/api-docs/**").permitAll() // API 문서
				.requestMatchers("/api/**").permitAll()
				.anyRequest().authenticated()
			)
			// 커스텀 예외 처리 핸들러 설정
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(customAuthenticationEntryPoint) // 인증 실패 시
				.accessDeniedHandler(customAccessDeniedHandler) // 권한 부족 시
			)
			.logout(logout -> logout.logoutSuccessUrl("/")) //로그아웃 시 리다이렉트될 URL을 설정
			.oauth2Login(oauth2Login -> oauth2Login
				.defaultSuccessUrl("/")// OAuth 2 로그인 설정 진입점
				.successHandler(oAuth2AuthenticationSuccessHandler)
				.failureHandler(oAuth2AuthenticationFailureHandler)
				.userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
					.userService(customOAuth2UserService) // OAuth 2 로그인 성공 이후 사용자 정보를 가져올 때의 설정
				)
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
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
