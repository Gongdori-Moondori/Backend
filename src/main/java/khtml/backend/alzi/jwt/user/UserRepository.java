package khtml.backend.alzi.jwt.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByUserId(String userId);
	boolean existsByUserId(String userId);
	Optional<User> findBySocialProviderAndSocialId(String socialProvider, String socialId);
}
