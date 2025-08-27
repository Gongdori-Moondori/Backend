package khtml.backend.alzi.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/test")
@Slf4j
public class OAuth2TestController {

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;
    
    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;
    
    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @GetMapping("/kakao-config")
    public ResponseEntity<String> testKakaoConfig() {
        StringBuilder config = new StringBuilder();
        config.append("Kakao OAuth2 Configuration:\n");
        config.append("Client ID: ").append(kakaoClientId).append("\n");
        config.append("Client Secret: ").append(kakaoClientSecret != null ? "설정됨" : "없음").append("\n");
        config.append("Redirect URI: ").append(kakaoRedirectUri).append("\n");
        config.append("Authorization URL: https://kauth.kakao.com/oauth/authorize\n");
        config.append("Token URL: https://kauth.kakao.com/oauth/token\n");
        
        return ResponseEntity.ok(config.toString());
    }
    
    @GetMapping("/manual-kakao")
    public ResponseEntity<String> manualKakaoAuth() {
        String authUrl = String.format(
            "https://kauth.kakao.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code&scope=profile_nickname,account_email",
            kakaoClientId, 
            kakaoRedirectUri
        );
        
        return ResponseEntity.ok("Manual Kakao Auth URL: " + authUrl);
    }
    
    @GetMapping("/callback")
    public ResponseEntity<String> testCallback(@RequestParam(required = false) String code,
                                             @RequestParam(required = false) String error) {
        if (error != null) {
            return ResponseEntity.badRequest().body("OAuth Error: " + error);
        }
        
        if (code != null) {
            return ResponseEntity.ok("Authorization Code received: " + code);
        }
        
        return ResponseEntity.badRequest().body("No code or error parameter found");
    }
}
