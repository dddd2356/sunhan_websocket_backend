package kakao.login.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kakao.login.dto.request.auth.*;
import kakao.login.dto.response.auth.*;
import org.springframework.http.ResponseEntity;

// 인증 관련 서비스 인터페이스 정의
public interface AuthService {

    // 아이디 중복 체크
    ResponseEntity<? super IdCheckResponseDto> idCheck(IdCheckRequestDto dto);

    // 이메일 인증 요청
    ResponseEntity<? super EmailCertificationResponseDto> emailCertification(EmailCertificationRequestDto dto);

    // 이메일 인증 여부 확인
    ResponseEntity<? super CheckCertificationResponseDto> checkCertification(CheckCertificationRequestDto dto);

    // 회원가입 처리
    ResponseEntity<? super SignUpResponseDto> signUp(SignUpRequestDto dto);

    // 로그인 처리
    ResponseEntity<? super SignInResponseDto> signIn(SignInRequestDto dto);

    // 로그아웃 처리
    ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response, String loginMethod);

    // 새로운 메서드: 토큰 갱신
    ResponseEntity<? super RefreshTokenResponseDto> refreshToken(RefreshTokenRequestDto requestDto);
    boolean validateToken(String token);
}
