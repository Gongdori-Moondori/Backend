package khtml.backend.alzi.jwt;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import khtml.backend.alzi.exception.CustomException;
import khtml.backend.alzi.exception.ErrorCode;
import khtml.backend.alzi.jwt.user.User;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtTokenProvider {

	@Value("${spring.jwt.secret}")
	private String jwtSecret;

	@Value("${spring.jwt.access-token-expire-time}")
	private long jwtExpirationInMs;
	
	@Value("${spring.jwt.refresh-token-expire-time}")
	private long refreshExpirationInMs;

	public String generateToken(User user) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

		return Jwts.builder()
			.setSubject(user.getUserId())
			.setIssuedAt(new Date())
			.setExpiration(expiryDate)
			.claim("type", "access")
			.signWith(SignatureAlgorithm.HS512, jwtSecret)
			.compact();
	}
	
	public String generateRefreshToken(User user) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + refreshExpirationInMs);

		return Jwts.builder()
			.setSubject(user.getUserId())
			.setIssuedAt(new Date())
			.setExpiration(expiryDate)
			.claim("type", "refresh")
			.signWith(SignatureAlgorithm.HS512, jwtSecret)
			.compact();
	}
	
	public long getAccessTokenExpirationInSeconds() {
		return jwtExpirationInMs / 1000;
	}

	public String getUserIdFromRefreshToken(String refreshToken) {
		try {
			Claims claims = Jwts.parser()
				.setSigningKey(jwtSecret)
				.parseClaimsJws(refreshToken)
				.getBody();

			return claims.getSubject();
		} catch (ExpiredJwtException e) {
			// 만료된 refresh token이라도 사용자 ID는 추출 가능
			log.debug("Refresh token is expired but extracting user ID: {}", e.getMessage());
			return e.getClaims().getSubject();
		} catch (UnsupportedJwtException e) {
			log.error("JWT token is unsupported: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
		} catch (MalformedJwtException e) {
			log.error("JWT token is malformed: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
		} catch (SignatureException e) {
			log.error("JWT signature does not match: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
		} catch (IllegalArgumentException e) {
			log.error("JWT token compact of handler are invalid: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
		}
	}

	public String getUserIdFromJWT(String token) {
		try {
			Claims claims = Jwts.parser()
				.setSigningKey(jwtSecret)
				.parseClaimsJws(token)
				.getBody();

			return claims.getSubject();
		} catch (ExpiredJwtException e) {
			log.error("JWT token is expired: {}", e.getMessage());
			throw new CustomException(ErrorCode.EXPIRED_JWT_TOKEN);
		} catch (UnsupportedJwtException e) {
			log.error("JWT token is unsupported: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
		} catch (MalformedJwtException e) {
			log.error("JWT token is malformed: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
		} catch (SignatureException e) {
			log.error("JWT signature does not match: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
		} catch (IllegalArgumentException e) {
			log.error("JWT token compact of handler are invalid: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
		}
	}

	public boolean validateToken(String authToken) {
		try {
			Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
			return true;
		} catch (ExpiredJwtException e) {
			log.error("JWT token is expired: {}", e.getMessage());
			throw new CustomException(ErrorCode.EXPIRED_JWT_TOKEN);
		} catch (UnsupportedJwtException e) {
			log.error("JWT token is unsupported: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
		} catch (MalformedJwtException e) {
			log.error("JWT token is malformed: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
		} catch (SignatureException e) {
			log.error("JWT signature does not match: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
		} catch (IllegalArgumentException e) {
			log.error("JWT token compact of handler are invalid: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
		}
	}

	public boolean validateRefreshToken(String refreshToken) {
		try {
			log.debug("리프레시 토큰 파싱 시작");
			
			Claims claims = Jwts.parser()
				.setSigningKey(jwtSecret)
				.parseClaimsJws(refreshToken)
				.getBody();
			
			log.debug("리프레시 토큰 파싱 성공");
			
			// 리프레시 토큰인지 확인
			String tokenType = claims.get("type", String.class);
			if (!"refresh".equals(tokenType)) {
				log.error("토큰 타입이 refresh가 아님: {}", tokenType);
				return false;
			}
			
			// 만료 시간 확인
			Date expiration = claims.getExpiration();
			if (expiration.before(new Date())) {
				log.error("리프레시 토큰이 만료됨: {}", expiration);
				return false;
			}
			
			log.debug("리프레시 토큰 검증 성공");
			return true;
			
		} catch (ExpiredJwtException e) {
			log.error("Refresh token is expired: {}", e.getMessage());
			return false;
		} catch (UnsupportedJwtException e) {
			log.error("Refresh token is unsupported: {}", e.getMessage());
			return false;
		} catch (MalformedJwtException e) {
			log.error("Refresh token is malformed: {}", e.getMessage());
			return false;
		} catch (SignatureException e) {
			log.error("Refresh token signature does not match: {}", e.getMessage());
			return false;
		} catch (IllegalArgumentException e) {
			log.error("Refresh token is invalid: {}", e.getMessage());
			return false;
		} catch (Exception e) {
			log.error("Unexpected error validating refresh token: {}", e.getMessage());
			return false;
		}
	}
	
	public boolean isTokenExpired(String token) {
		try {
			Claims claims = Jwts.parser()
				.setSigningKey(jwtSecret)
				.parseClaimsJws(token)
				.getBody();
			
			return claims.getExpiration().before(new Date());
		} catch (ExpiredJwtException e) {
			return true;
		} catch (Exception e) {
			return true;
		}
	}
}
