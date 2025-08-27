package khtml.backend.alzi.auth.oauth2;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import khtml.backend.alzi.auth.user.User;
import khtml.backend.alzi.auth.user.UserDTO;
import khtml.backend.alzi.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final UserRepository userRepository;
	private final HttpSession httpSession;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

		System.out.println("=== OAuth2 로그인 시작 ===");

		OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
		OAuth2User oAuth2User = delegate.loadUser(userRequest);

		// 현재 로그인 진행 중인 서비스를 구분하는 코드 (네이버 로그인인지 구글 로그인인지 구분)
		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		System.out.println("소셜 제공자: " + registrationId);

		// OAuth2 로그인 진행 시 키가 되는 필드 값 (Primary Key와 같은 의미)을 의미
		// 구글의 기본 코드는 "sub", 후에 네이버 로그인과 구글 로그인을 동시 지원할 때 사용
		String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
			.getUserInfoEndpoint().getUserNameAttributeName();
		System.out.println("userNameAttributeName: " + userNameAttributeName);
		System.out.println("OAuth2User attributes: " + oAuth2User.getAttributes());

		// OAuth2UserService를 통해 가져온 OAuthUser의 attribute를 담을 클래스 ( 네이버 등 다른 소셜 로그인도 이 클래스 사용)
		OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName,
			oAuth2User.getAttributes());

		System.out.println("파싱된 attributes - name: " + attributes.getName() + ", email: " + attributes.getEmail());

		User userEntity = saveOrUpdate(attributes);
		System.out.println("최종 사용자 엔티티 - ID: " + userEntity.getUserId() + ", Email: " + userEntity.getEmail());

		// UserEntity 클래스를 사용하지 않고 SessionUser클래스를 사용하는 이유는 오류 방지.
		httpSession.setAttribute("user", new UserDTO(userEntity)); // UserDTO : 세션에 사용자 정보를 저장하기 위한 Dto 클래스

		System.out.println("=== OAuth2 로그인 완료 ===");

		return new DefaultOAuth2User(
			Collections.singleton(new SimpleGrantedAuthority(userEntity.getUserId())),
			attributes.getAttributes(),
			attributes.getNameAttributeKey());
	}

	private User saveOrUpdate(OAuthAttributes attributes) {

		// 이메일을 기준으로 사용자를 찾아 업데이트하거나, 사용자를 새로 생성합니다.
		System.out.println("saveOrUpdate - 조회할 이메일: " + attributes.getEmail());

		User userEntity = userRepository.findBySocialProviderAndSocialId(attributes.getSocialProvider(),
				attributes.getSocialId())
			.map(entity -> {
				System.out.println("기존 사용자 발견 - ID: " + entity.getUserId() + ", Email: " + entity.getEmail());
				return entity.update(attributes.getName(), attributes.getProfileImage());
			})
			.orElseGet(() -> {
				System.out.println("새 사용자 생성 - Email: " + attributes.getEmail());
				return attributes.toEntity();
			});

		User savedUser = userRepository.save(userEntity);
		System.out.println("저장된 사용자 - ID: " + savedUser.getUserId() + ", Email: " + savedUser.getEmail());

		return savedUser;
	}
}