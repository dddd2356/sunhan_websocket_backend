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

@RestController  // RESTful API 컨트롤러
@RequestMapping("/api/v1/auth")  // URL 경로 설정
@RequiredArgsConstructor  // 생성자 주입을 위한 Lombok 어노테이션
public class AuthController {

    private final AuthService authService;  // 인증 서비스 주입

    // ID 중복 확인 엔드포인트
    @PostMapping("/id-check")
    public ResponseEntity<? super IdCheckResponseDto> idCheck(@RequestBody @Valid IdCheckRequestDto requestBody){
        ResponseEntity<? super IdCheckResponseDto> response = authService.idCheck(requestBody);  // 인증 서비스 호출
        return response;  // 응답 반환
    }

    // 이메일 인증 요청 엔드포인트
    @PostMapping("/email-certification")
    public ResponseEntity<? super EmailCertificationResponseDto> emailCertification(@RequestBody @Valid EmailCertificationRequestDto requestBody){
        ResponseEntity<? super EmailCertificationResponseDto> response = authService.emailCertification(requestBody);
        return response;
    }

    // 인증 번호 확인 요청 엔드포인트
    @PostMapping("/check-certification")
    public ResponseEntity<? super CheckCertificationResponseDto> checkCertification(@RequestBody @Valid CheckCertificationRequestDto requestBody){
        ResponseEntity<? super CheckCertificationResponseDto> response = authService.checkCertification(requestBody);
        return response;
    }

    // 회원가입 엔드포인트
    @PostMapping("/sign-up")
    public ResponseEntity<? super SignUpResponseDto> signUp(@RequestBody @Valid SignUpRequestDto requestBody){
        ResponseEntity<? super SignUpResponseDto> response = authService.signUp(requestBody);
        return response;
    }

    // 로그인 엔드포인트
    @PostMapping("/sign-in")
    public ResponseEntity<? super SignInResponseDto> signIn(@RequestBody @Valid SignInRequestDto requestBody){
        ResponseEntity<? super SignInResponseDto> response = authService.signIn(requestBody);
        return response;
    }

    // 카카오 로그아웃 엔드포인트
    @PostMapping("/logout/kakao")
    public ResponseEntity<String> logoutKakao(HttpServletResponse response) {
        // 카카오 로그아웃 시 쿠키 삭제
        ResponseCookie cookie = ResponseCookie.from("kakaoAccessToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("None")
                .secure(true)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());  // 쿠키 추가
        return ResponseEntity.ok("로그아웃 완료");
    }

    // 웹(JWT) 로그아웃 엔드포인트
    @PostMapping("/logout/web")
    public ResponseEntity<String> logoutWeb(HttpServletResponse response) {
        // JWT 토큰을 저장하는 쿠키 삭제
        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("None")
                .secure(true)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());  // 쿠키 추가
        return ResponseEntity.ok("로그아웃 완료");
    }
}
