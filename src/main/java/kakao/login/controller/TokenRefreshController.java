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
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class TokenRefreshController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequestDto requestBody) {
        log.info("토큰 갱신 요청 수신");

        if (requestBody.getRefreshToken() == null || requestBody.getRefreshToken().isEmpty()) {
            return RefreshTokenResponseDto.refreshTokenNotProvided();
        }

        log.info("토큰 갱신 요청 수신: {}...", requestBody.getRefreshToken().substring(0, 10));

        return authService.refreshToken(requestBody);
    }


    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "message", "Authorization header must start with Bearer"));
        }

        String token = authHeader.replace("Bearer ", "");
        if (token.length() > 10) {
            log.info("토큰 검증 요청: {}...", token.substring(0, 10));
        }

        boolean isValid = authService.validateToken(token);

        if (isValid) {
            log.info("토큰 유효함");
            return ResponseEntity.ok().body(Map.of("valid", true));
        } else {
            log.info("토큰 유효하지 않음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
        }
    }
}