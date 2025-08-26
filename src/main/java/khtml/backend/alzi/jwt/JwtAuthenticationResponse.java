package khtml.backend.alzi.jwt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class JwtAuthenticationResponse {
	private String accessToken;
	private String refreshToken;
	@Builder.Default
	private String tokenType = "Bearer";
	private Long expiresIn; // 액세스 토큰 만료 시간 (초)

	public JwtAuthenticationResponse(String accessToken) {
		this.accessToken = accessToken;
		this.tokenType = "Bearer";
	}
	
	public JwtAuthenticationResponse(String accessToken, String refreshToken, String tokenType, Long expiresIn) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.tokenType = tokenType;
		this.expiresIn = expiresIn;
	}
}
