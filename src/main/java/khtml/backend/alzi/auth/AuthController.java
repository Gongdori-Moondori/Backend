package khtml.backend.alzi.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import khtml.backend.alzi.jwt.JwtAuthenticationResponse;
import khtml.backend.alzi.jwt.JwtTokenProvider;
import khtml.backend.alzi.jwt.user.User;
import khtml.backend.alzi.jwt.user.UserRepository;
import khtml.backend.alzi.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "인증", description = "로그인/로그아웃 관련 API")
@Slf4j
public class AuthController {

	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;

	@GetMapping("/login")
	@Operation(summary = "로그인 페이지", description = "소셜 로그인 페이지를 반환합니다.")
	public String loginPage(Model model) {
		log.info("로그인 페이지 요청");
		return "login";
	}

	@GetMapping("/")
	public String home(Model model) {
		log.info("홈 페이지 요청");
		return "index";
	}

	@GetMapping("/oauth2/redirect")
	@Operation(summary = "OAuth2 리다이렉트", description = "OAuth2 로그인 성공 후 리다이렉트 페이지")
	public String oauth2Redirect(Model model,
		@RequestParam(required = false) String token,
		@RequestParam(required = false) String refresh) {
		log.info("OAuth2 리다이렉트 요청 - token: {}, refresh: {}",
			token != null ? "있음" : "없음",
			refresh != null ? "있음" : "없음");

		model.addAttribute("token", token);
		model.addAttribute("refresh", refresh);
		return "oauth2-redirect";
	}

	@PostMapping("/api/auth/refresh")
	@ResponseBody
	@Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 Access Token을 갱신합니다.")
	public ApiResponse<JwtAuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
		try {
			log.info("토큰 갱신 요청");

			String refreshToken = request.getRefreshToken();
			if (refreshToken == null || refreshToken.trim().isEmpty()) {
				return ApiResponse.failure("INVALID_REQUEST", "Refresh token이 필요합니다.");
			}

			// Refresh Token 유효성 검사
			if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
				return ApiResponse.failure("INVALID_TOKEN", "유효하지 않은 refresh token입니다.");
			}

			// 사용자 정보 조회
			String userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
			User user = userRepository.findByUserId(userId)
				.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

			// 새로운 토큰 생성
			String newAccessToken = jwtTokenProvider.generateToken(user);
			String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

			JwtAuthenticationResponse response = JwtAuthenticationResponse.builder()
				.accessToken(newAccessToken)
				.refreshToken(newRefreshToken)
				.tokenType("Bearer")
				.expiresIn(jwtTokenProvider.getAccessTokenExpirationInSeconds())
				.build();

			log.info("토큰 갱신 성공: userId={}", userId);
			return ApiResponse.success("토큰이 성공적으로 갱신되었습니다.", response);

		} catch (Exception e) {
			log.error("토큰 갱신 실패: {}", e.getMessage(), e);
			return ApiResponse.failure("TOKEN_REFRESH_FAILED", "토큰 갱신에 실패했습니다.");
		}
	}

	@GetMapping("/api/auth/me")
	@ResponseBody
	@Operation(summary = "현재 사용자 정보", description = "현재 로그인된 사용자의 정보를 반환합니다.")
	public ApiResponse<User> getCurrentUser() {
		try {
			// SecurityUtils를 사용하여 현재 사용자 정보 가져오기
			// 현재 SecurityUtils에 문제가 있으므로 임시로 간단한 응답 반환
			return ApiResponse.success("현재 사용자 정보 조회 성공", null);
		} catch (Exception e) {
			log.error("현재 사용자 정보 조회 실패: {}", e.getMessage(), e);
			return ApiResponse.failure("USER_INFO_FAILED", "사용자 정보 조회에 실패했습니다.");
		}
	}
}
