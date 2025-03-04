package kakao.login.controller;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kakao.login.dto.request.auth.*;
import kakao.login.dto.response.auth.*;
import kakao.login.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/id-check")
    public ResponseEntity<? super IdCheckResponseDto> idCheck(@RequestBody @Valid IdCheckRequestDto requestBody){
        ResponseEntity<? super IdCheckResponseDto> response = authService.idCheck(requestBody);
        return response;
    }

    @PostMapping("/email-certification")
    public ResponseEntity<? super EmailCertificationResponseDto> emailCertification(@RequestBody @Valid EmailCertificationRequestDto requestBody){
        ResponseEntity<? super EmailCertificationResponseDto> response = authService.emailCertification(requestBody);
        return response;
    }

    @PostMapping("/check-certification")
    public ResponseEntity<? super CheckCertificationResponseDto> checkCertification(@RequestBody @Valid CheckCertificationRequestDto requestBody){
        ResponseEntity<? super CheckCertificationResponseDto> response = authService.checkCertification(requestBody);
        return response;
    }

    @PostMapping("/sign-up")
    public ResponseEntity<? super SignUpResponseDto> signUp(@RequestBody @Valid SignUpRequestDto requestBody){
        ResponseEntity<? super SignUpResponseDto> response = authService.signUp(requestBody);
        return response;
    }

    @PostMapping("/sign-in")
    public ResponseEntity<? super SignInResponseDto> signIn(@RequestBody @Valid SignInRequestDto requestBody){
        ResponseEntity<? super SignInResponseDto> response = authService.signIn(requestBody);
        return response;
    }


     //✅ 카카오 로그아웃 엔드포인트
    @PostMapping("/logout/kakao")
    public ResponseEntity<String> logoutKakao(HttpServletResponse response) {
        // 쿠키 삭제 설정
        ResponseCookie cookie = ResponseCookie.from("kakaoAccessToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("None")
                .secure(true)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok("로그아웃 완료");
    }

    // 웹(JWT) 로그아웃 엔드포인트 (단순 클라이언트 토큰 삭제)
    @PostMapping("/logout/web")
    public ResponseEntity<String> logoutWeb(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok("웹 로그아웃 완료");
    }

    // 웹(JWT) 로그아웃 엔드포인트 (단순 클라이언트 토큰 삭제)
    @PostMapping("/logout/naver")
    public ResponseEntity<String> logoutNaver(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("naverAccessToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok("네이버 로그아웃 완료");
    }

}
