package khtml.backend.alzi.jwt.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String userId;

	private String name;
	private String socialProvider;
	private String socialId;
	private String email;
	private String profileImage;

	@Column(nullable = false)
	private String password;

	public User update(String name, String picture) {
		this.name = name;
		this.profileImage = picture;
		return this;
	}
}
