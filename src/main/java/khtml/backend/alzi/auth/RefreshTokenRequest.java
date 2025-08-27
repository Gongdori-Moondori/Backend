package khtml.backend.alzi.auth;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
