package khtml.backend.alzi.auth.jwt;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {

	public JwtAuthenticationToken(Object principal, Object credentials) {
		super(principal, credentials);
	}

	public JwtAuthenticationToken(Object principal, Object credentials,
		Collection<? extends GrantedAuthority> authorities) {
		super(principal, credentials, authorities);
	}
}
