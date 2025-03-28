package kakao.login.controller;

import kakao.login.dto.request.auth.RefreshTokenRequestDto;
import kakao.login.dto.response.auth.RefreshTokenResponseDto;
import kakao.login.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class TokenRefreshController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequestDto requestBody) {
        log.info("Token refresh request received");
        if (requestBody.getRefreshToken() == null || requestBody.getRefreshToken().isEmpty()) {
            log.warn("Refresh token not provided");
            return RefreshTokenResponseDto.refreshTokenNotProvided();
        }

        String refreshToken = requestBody.getRefreshToken();
        log.info("Refreshing token: {}...", refreshToken.substring(0, Math.min(10, refreshToken.length())));
        return authService.refreshToken(requestBody);
    }

    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header: {}", authHeader);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "message", "Authorization header must start with Bearer"));
        }

        String token = authHeader.replace("Bearer ", "");
        log.info("Validating token: {}...", token.substring(0, Math.min(10, token.length())));

        boolean isValid = authService.validateToken(token);
        if (isValid) {
            log.info("Token is valid");
            return ResponseEntity.ok().body(Map.of("valid", true));
        } else {
            log.warn("Token is invalid or expired");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "message", "Token is invalid or has expired"));
        }
    }
}