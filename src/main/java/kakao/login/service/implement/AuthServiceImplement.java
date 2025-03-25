package kakao.login.service.implement;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kakao.login.common.CertificationNumber;
import kakao.login.dto.request.auth.*;
import kakao.login.dto.response.ResponseDto;
import kakao.login.dto.response.auth.*;
import kakao.login.entity.CertificationEntity;
import kakao.login.entity.RefreshTokenEntity;
import kakao.login.entity.UserEntity;
import kakao.login.provider.EmailProvider;
import kakao.login.provider.JwtProvider;
import kakao.login.repository.CertificationRepository;
import kakao.login.repository.UserRepository;
import kakao.login.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImplement implements AuthService {

    // 의존성 주입: 각 서비스 및 리포지토리 클래스
    private final UserRepository userRepository;
    private final CertificationRepository certificationRepository;
    private final JwtProvider jwtProvider;
    private final EmailProvider emailProvider;

    // RefreshTokenService 주입
    private final RefreshTokenServiceImplement refreshTokenService;

    // 로그아웃을 위해 RestTemplate을 사용하여 외부 API 호출
    private final RestTemplate restTemplate = new RestTemplate();

    // 비밀번호 암호화를 위해 BCryptPasswordEncoder 사용
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 아이디 중복 체크
    @Override
    public ResponseEntity<? super IdCheckResponseDto> idCheck(IdCheckRequestDto dto) {
        try {
            String userId = dto.getId();
            boolean isExistId = userRepository.existsByUserId(userId);
            if (isExistId) return IdCheckResponseDto.duplicateId();  // 아이디가 중복되면 오류 응답
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();  // 예외 발생 시 DB 오류 응답
        }
        return IdCheckResponseDto.success();  // 성공 응답
    }


    // 이메일 인증
    @Override
    public ResponseEntity<? super EmailCertificationResponseDto> emailCertification(EmailCertificationRequestDto dto) {
        try {
            String userId = dto.getId();
            String email = dto.getEmail();

            // 이미 존재하는 유저인지 확인
            boolean isExistId = userRepository.existsByUserId(userId);
            if (isExistId) return EmailCertificationResponseDto.duplicateId();  // 중복된 아이디 응답

            // 인증번호 생성 및 이메일 전송
            String certificationNumber = CertificationNumber.getCertificationNumber();
            boolean isSuccessed = emailProvider.sendCertificationMail(email, certificationNumber);
            if (!isSuccessed) return EmailCertificationResponseDto.mailSendFail();  // 메일 전송 실패 시 응답

            // 인증 정보 저장
            CertificationEntity certificationEntity = new CertificationEntity(userId, email, certificationNumber);
            certificationRepository.save(certificationEntity);

        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();  // 예외 발생 시 DB 오류 응답
        }

        return EmailCertificationResponseDto.success();  // 인증 성공 응답
    }

    // 이메일 인증 체크
    @Override
    public ResponseEntity<? super CheckCertificationResponseDto> checkCertification(CheckCertificationRequestDto dto) {
        try {
            String userId = dto.getId();
            String email = dto.getEmail();
            String certificationNumber = dto.getCertificationNumber();

            // 인증 정보 확인
            CertificationEntity certificationEntity = certificationRepository.findByUserId(userId);
            if (certificationEntity == null) return CheckCertificationResponseDto.certificationFail();  // 인증 실패 응답

            boolean isMatched = certificationEntity.getEmail().equals(email) && certificationEntity.getCertificationNumber().equals(certificationNumber);
            if (!isMatched) return CheckCertificationResponseDto.certificationFail();  // 인증번호 일치하지 않으면 실패

        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();  // 예외 발생 시 DB 오류 응답
        }

        return CheckCertificationResponseDto.success();  // 인증 성공 응답
    }

    // 회원가입 처리
    @Override
    public ResponseEntity<? super SignUpResponseDto> signUp(SignUpRequestDto dto) {
        try {
            // 이미 존재하는 유저아이디인지 체크
            String userId = dto.getId();
            boolean isExistId = userRepository.existsByUserId(userId);
            if (isExistId) return SignUpResponseDto.duplicateId();  // 중복된 아이디 응답

            // 이메일 인증 여부 체크
            String email = dto.getEmail();
            String certificationNumber = dto.getCertificationNumber();
            CertificationEntity certificationEntity = certificationRepository.findByUserId(userId);
            boolean isMatched = certificationEntity.getEmail().equals(email) && certificationEntity.getCertificationNumber().equals(certificationNumber);
            if (!isMatched) return SignUpResponseDto.certificationFail();  // 인증 실패 응답

            // 비밀번호 암호화 처리
            String password = dto.getPassword();
            String encodedPassword = passwordEncoder.encode(password);
            dto.setPassword(encodedPassword);

            // 유저 엔티티 저장
            UserEntity userEntity = new UserEntity(dto);
            userRepository.save(userEntity);

            // 인증 정보 삭제
            certificationRepository.deleteByUserId(userId);

        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();  // 예외 발생 시 DB 오류 응답
        }

        return SignUpResponseDto.success();  // 회원가입 성공 응답
    }

    // 로그인 처리
    @Override
    public ResponseEntity<? super SignInResponseDto> signIn(SignInRequestDto dto) {
        String token = null;
        String refreshToken = null;
        long expiresIn = 0;

        try {
            String userId = dto.getId();
            UserEntity userEntity = userRepository.findByUserId(userId);
            if (userEntity == null) return SignInResponseDto.signInFail();

            // 비밀번호 일치 확인
            String password = dto.getPassword();
            String encodedPassword = userEntity.getPassword();
            boolean isMatched = passwordEncoder.matches(password, encodedPassword);
            if (!isMatched) return SignInResponseDto.signInFail();

            // JWT 액세스 토큰 생성
            token = jwtProvider.create(userId, userEntity.getRole());

            // 리프레시 토큰 생성 및 DB에 저장 (RefreshTokenService 사용)
            RefreshTokenEntity refreshTokenEntity = refreshTokenService.createRefreshToken(userId);
            refreshToken = refreshTokenEntity.getToken();

            // 토큰 만료 시간 (초 단위)
            expiresIn = jwtProvider.getAccessTokenExpirationTime();

        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }

        // 반환하는 응답 객체에 access token, refresh token, 만료 시간 모두 포함
        return SignInResponseDto.success(token, refreshToken, expiresIn);
    }


    // 로그아웃 처리
    @Override
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response, String loginMethod) {
        String token = null;
        Cookie[] cookies = request.getCookies();

        // 세션 무효화
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();  // 세션 무효화
        }

        // 세션 쿠키 삭제
        Cookie sessionCookie = new Cookie("Idea-b5e63b4f", null);  // 세션 쿠키 이름
        sessionCookie.setMaxAge(0);  // 쿠키 만료
        sessionCookie.setDomain("localhost:4040");  // 도메인 설정
        sessionCookie.setPath("/");  // 도메인 범위 설정
        sessionCookie.setSecure(false);  // 로컬 환경에서는 Secure를 false로 설정
        sessionCookie.setHttpOnly(true);  // HttpOnly 설정
        response.addCookie(sessionCookie);

        // 쿠키에서 토큰 추출
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("kakaoToken".equals(cookie.getName()) && "kakao".equals(loginMethod)) {
                    token = cookie.getValue();
                    break;
                } else if ("naverToken".equals(cookie.getName()) && "naver".equals(loginMethod)) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loginMethod + " 토큰이 없습니다.");
        }

        // 로그인 방식에 따라 로그아웃 처리
        if ("kakao".equals(loginMethod)) {
            return logoutKakao(token, response);
        } else if ("naver".equals(loginMethod)) {
            return logoutNaver(token, response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("지원되지 않는 로그인 방식입니다.");
        }
    }

    @Override
    public ResponseEntity<? super RefreshTokenResponseDto> refreshToken(RefreshTokenRequestDto requestDto) {
        String refreshToken = requestDto.getRefreshToken();
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return RefreshTokenResponseDto.refreshTokenNotProvided();
        }

        Optional<Map<String, Object>> tokenMapOpt = refreshTokenService.refreshAccessToken(refreshToken);
        if (tokenMapOpt.isEmpty()) {
            return RefreshTokenResponseDto.invalidRefreshToken();
        }

        Map<String, Object> tokenMap = tokenMapOpt.get();
        String newAccessToken = (String) tokenMap.get("accessToken");
        String newRefreshToken = (String) tokenMap.get("refreshToken");
        // jwtProvider.getAccessTokenExpirationTime()는 초 단위로 반환한다고 가정
        long expiresIn = jwtProvider.getAccessTokenExpirationTime();

        return RefreshTokenResponseDto.success(newAccessToken, newRefreshToken, expiresIn);
    }



    @Override
    public boolean validateToken(String token) {
        try {
            // jwtSecret 변수는 JwtProvider에서 관리해야 합니다.
            // 따라서 JwtProvider의 메서드를 호출하여 토큰 검증
            return jwtProvider.validateToken(token);
        } catch (Exception e) {
            log.error("토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    // 카카오 로그아웃 처리
    private ResponseEntity<String> logoutKakao(String kakaoToken, HttpServletResponse response) {
        String KAKAO_LOGOUT_URL = "https://kapi.kakao.com/v1/user/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + kakaoToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 카카오 API 호출하여 로그아웃 처리
        ResponseEntity<String> kakaoResponse = restTemplate.exchange(KAKAO_LOGOUT_URL, HttpMethod.POST, entity, String.class);

        if (kakaoResponse.getStatusCode() == HttpStatus.OK) {
            // 카카오 로그아웃 후 쿠키 삭제
            Cookie cookie = new Cookie("kakaoToken", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);

            return ResponseEntity.ok("카카오 로그아웃 성공");
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("카카오 로그아웃 실패");
    }

    // 네이버 로그아웃 처리
    private ResponseEntity<String> logoutNaver(String naverToken, HttpServletResponse response) {
        String NAVER_LOGOUT_URL = "https://nid.naver.com/oauth2.0/token?grant_type=delete&client_id=naver.client.id&client_secret=naver.client.secret&access_token=" + naverToken;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 네이버 API 호출하여 로그아웃 처리
        ResponseEntity<String> naverResponse = restTemplate.exchange(NAVER_LOGOUT_URL, HttpMethod.GET, entity, String.class);

        if (naverResponse.getStatusCode() == HttpStatus.OK) {
            // 네이버 로그아웃 후 쿠키 삭제
            Cookie cookie = new Cookie("naverToken", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);

            return ResponseEntity.ok("네이버 로그아웃 성공");
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("네이버 로그아웃 실패");
    }

}
