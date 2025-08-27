package khtml.backend.alzi.auth.user;

import java.io.Serializable;

import lombok.Getter;

@Getter
public class UserDTO implements Serializable {
	private String name;
	private String email;
	private String picture;
	private String provider;
	private String providerId;

	public UserDTO(User userEntity) {
		this.name = userEntity.getName();
		this.email = userEntity.getEmail();
		this.picture = userEntity.getProfileImage();
		this.provider = userEntity.getSocialProvider();
		this.providerId = userEntity.getSocialId();
	}
}