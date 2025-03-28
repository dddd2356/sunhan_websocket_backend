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

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImplement implements AuthService {

    private final UserRepository userRepository;
    private final CertificationRepository certificationRepository;
    private final JwtProvider jwtProvider;
    private final EmailProvider emailProvider;
    private final RefreshTokenServiceImplement refreshTokenService;
    private final RestTemplate restTemplate = new RestTemplate();
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public ResponseEntity<? super IdCheckResponseDto> idCheck(IdCheckRequestDto dto) {
        try {
            String userId = dto.getId();
            boolean isExistId = userRepository.existsByUserId(userId);
            if (isExistId) return IdCheckResponseDto.duplicateId();
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }
        return IdCheckResponseDto.success();
    }

    @Override
    public ResponseEntity<? super EmailCertificationResponseDto> emailCertification(EmailCertificationRequestDto dto) {
        try {
            String userId = dto.getId();
            String email = dto.getEmail();

            boolean isExistId = userRepository.existsByUserId(userId);
            if (isExistId) return EmailCertificationResponseDto.duplicateId();

            String certificationNumber = CertificationNumber.getCertificationNumber();
            boolean isSuccessed = emailProvider.sendCertificationMail(email, certificationNumber);
            if (!isSuccessed) return EmailCertificationResponseDto.mailSendFail();

            CertificationEntity certificationEntity = new CertificationEntity(userId, email, certificationNumber);
            certificationRepository.save(certificationEntity);
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }
        return EmailCertificationResponseDto.success();
    }

    @Override
    public ResponseEntity<? super CheckCertificationResponseDto> checkCertification(CheckCertificationRequestDto dto) {
        try {
            String userId = dto.getId();
            String email = dto.getEmail();
            String certificationNumber = dto.getCertificationNumber();

            CertificationEntity certificationEntity = certificationRepository.findByUserId(userId);
            if (certificationEntity == null) return CheckCertificationResponseDto.certificationFail();

            boolean isMatched = certificationEntity.getEmail().equals(email) && certificationEntity.getCertificationNumber().equals(certificationNumber);
            if (!isMatched) return CheckCertificationResponseDto.certificationFail();
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }
        return CheckCertificationResponseDto.success();
    }

    @Override
    public ResponseEntity<? super SignUpResponseDto> signUp(SignUpRequestDto dto) {
        try {
            String userId = dto.getId();
            boolean isExistId = userRepository.existsByUserId(userId);
            if (isExistId) return SignUpResponseDto.duplicateId();

            String email = dto.getEmail();
            String certificationNumber = dto.getCertificationNumber();
            CertificationEntity certificationEntity = certificationRepository.findByUserId(userId);
            boolean isMatched = certificationEntity.getEmail().equals(email) && certificationEntity.getCertificationNumber().equals(certificationNumber);
            if (!isMatched) return SignUpResponseDto.certificationFail();

            String password = dto.getPassword();
            String encodedPassword = passwordEncoder.encode(password);
            dto.setPassword(encodedPassword);

            UserEntity userEntity = new UserEntity(dto);
            userRepository.save(userEntity);

            certificationRepository.deleteByUserId(userId);
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }
        return SignUpResponseDto.success();
    }

    @Override
    public ResponseEntity<? super SignInResponseDto> signIn(SignInRequestDto dto) {
        try {
            String userId = dto.getId();
            UserEntity userEntity = userRepository.findByUserId(userId);
            if (userEntity == null) return SignInResponseDto.signInFail();

            String password = dto.getPassword();
            String encodedPassword = userEntity.getPassword();
            boolean isMatched = passwordEncoder.matches(password, encodedPassword);
            if (!isMatched) return SignInResponseDto.signInFail();

            String token = jwtProvider.create(userId, userEntity.getRole());
            String refreshToken = jwtProvider.createRefreshToken(userId);
            jwtProvider.saveRefreshToken(userEntity, refreshToken);
            long expiresIn = jwtProvider.getAccessTokenExpirationTime();
            log.info("SignIn - userId: {}, expiresIn: {} seconds", userId, expiresIn);

            return SignInResponseDto.success(token, refreshToken, expiresIn);
        } catch (Exception exception) {
            log.error("SignIn error", exception);
            return ResponseDto.databaseError();
        }
    }

    @Override
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response, String loginMethod) {
        String token = null;
        Cookie[] cookies = request.getCookies();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        Cookie sessionCookie = new Cookie("Idea-b5e63b4f", null);
        sessionCookie.setMaxAge(0);
        sessionCookie.setDomain("localhost:4040");
        sessionCookie.setPath("/");
        sessionCookie.setSecure(false);
        sessionCookie.setHttpOnly(true);
        response.addCookie(sessionCookie);

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
            log.warn("Refresh token not provided");
            return RefreshTokenResponseDto.refreshTokenNotProvided();
        }

        if (!jwtProvider.verifyRefreshToken(refreshToken)) {
            log.warn("Invalid or revoked refresh token: {}", refreshToken.substring(0, 10));
            return RefreshTokenResponseDto.invalidRefreshToken();
        }

        Optional<String> userIdOpt = jwtProvider.validateRefreshToken(refreshToken);
        if (userIdOpt.isEmpty()) {
            log.warn("No valid user ID in refresh token");
            return RefreshTokenResponseDto.invalidRefreshToken();
        }

        String userId = userIdOpt.get();
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", userId);
            return RefreshTokenResponseDto.userNotFound();
        }

        UserEntity user = userOpt.get();
        String newAccessToken = jwtProvider.create(userId, user.getRole());
        String newRefreshToken = jwtProvider.createRefreshToken(userId); // Generate new refresh token
        jwtProvider.saveRefreshToken(user, newRefreshToken); // Save new refresh token
        jwtProvider.revokeRefreshToken(refreshToken); // Revoke old refresh token
        long expiresIn = jwtProvider.getAccessTokenExpirationTime();

        log.info("Token refreshed successfully for user: {}", userId);
        return RefreshTokenResponseDto.success(newAccessToken, newRefreshToken, expiresIn);
    }

    @Override
    public boolean validateToken(String token) {
        return jwtProvider.validateToken(token);
    }

    private ResponseEntity<String> logoutKakao(String kakaoToken, HttpServletResponse response) {
        String KAKAO_LOGOUT_URL = "https://kapi.kakao.com/v1/user/logout";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + kakaoToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> kakaoResponse = restTemplate.exchange(KAKAO_LOGOUT_URL, HttpMethod.POST, entity, String.class);
        if (kakaoResponse.getStatusCode() == HttpStatus.OK) {
            Cookie cookie = new Cookie("kakaoToken", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
            return ResponseEntity.ok("카카오 로그아웃 성공");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("카카오 로그아웃 실패");
    }

    private ResponseEntity<String> logoutNaver(String naverToken, HttpServletResponse response) {
        String NAVER_LOGOUT_URL = "https://nid.naver.com/oauth2.0/token?grant_type=delete&client_id=naver.client.id&client_secret=naver.client.secret&access_token=" + naverToken;
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> naverResponse = restTemplate.exchange(NAVER_LOGOUT_URL, HttpMethod.GET, entity, String.class);
        if (naverResponse.getStatusCode() == HttpStatus.OK) {
            Cookie cookie = new Cookie("naverToken", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
            return ResponseEntity.ok("네이버 로그아웃 성공");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("네이버 로그아웃 실패");
    }
}