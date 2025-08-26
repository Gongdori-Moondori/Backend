package khtml.backend.alzi.jwt.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import khtml.backend.alzi.exception.CustomException;
import khtml.backend.alzi.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
		log.info("loadUserByUsername 호출: userId={}", userId);
		
		if (userId == null || userId.trim().isEmpty()) {
			log.error("userId가 비어있음");
			throw new UsernameNotFoundException("UserId cannot be empty");
		}
		
		try {
			User user = userRepository.findByUserId(userId)
				.orElseThrow(() -> {
					log.error("사용자를 찾을 수 없음: userId={}", userId);
					return new UsernameNotFoundException("User not found with userId: " + userId);
				});
			
			log.info("사용자 찾기 성공: userId={}", user.getUserId());
			return new CustomUserDetails(user);
			
		} catch (UsernameNotFoundException e) {
			throw e;
		} catch (Exception e) {
			log.error("사용자 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage(), e);
			throw new UsernameNotFoundException("Error occurred while loading user: " + userId);
		}
	}

	public User findByUserName(String userName) {
		if (userName == null || userName.trim().isEmpty()) {
			log.warn("findByUserName 호출 시 userName이 비어있음");
			return null;
		}
		
		try {
			return userRepository.findByUserId(userName).orElse(null);
		} catch (Exception e) {
			log.error("사용자 조회 실패: userName={}, error={}", userName, e.getMessage(), e);
			return null;
		}
	}
	
	public User findByUserId(String userId) {
		if (userId == null || userId.trim().isEmpty()) {
			log.warn("findByUserId 호출 시 userId가 비어있음");
			return null;
		}
		
		try {
			return userRepository.findByUserId(userId).orElse(null);
		} catch (Exception e) {
			log.error("사용자 조회 실패: userId={}, error={}", userId, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * 안전한 사용자 조회 (예외 발생)
	 */
	public User findByUserIdSafe(String userId) {
		if (userId == null || userId.trim().isEmpty()) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "사용자 ID가 필요합니다.");
		}
		
		try {
			return userRepository.findByUserId(userId)
				.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error("사용자 조회 실패: userId={}, error={}", userId, e.getMessage(), e);
			throw new CustomException(ErrorCode.DATABASE_ERROR, "사용자 조회 중 오류가 발생했습니다.");
		}
	}

	/**
	 * 사용자 존재 여부 확인
	 */
	public boolean existsByUserId(String userId) {
		if (userId == null || userId.trim().isEmpty()) {
			return false;
		}
		
		try {
			return userRepository.existsByUserId(userId);
		} catch (Exception e) {
			log.error("사용자 존재 확인 실패: userId={}, error={}", userId, e.getMessage(), e);
			return false;
		}
	}
}
