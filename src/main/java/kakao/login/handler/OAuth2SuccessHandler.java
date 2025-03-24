package kakao.login.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kakao.login.entity.CustomOAuth2User;
import kakao.login.entity.UserEntity;
import kakao.login.provider.JwtProvider;
import kakao.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    // JWT 생성 책임을 맡고 있는 JwtProvider
    private final JwtProvider jwtProvider;
    // 카카오 및 네이버 OAuth 인증된 클라이언트 서비스 관리
    private final OAuth2AuthorizedClientService authorizedClientService;
    // UserRepository 주입 (UserEntity 조회용)
    private final UserRepository userRepository;
    // 인증 성공 시 호출되는 메서드
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 인증된 사용자 정보를 CustomOAuth2User 객체로 캐스팅
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        // OAuth 인증 토큰을 가져옴
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        String userId = oAuth2User.getName();  // OAuth2 사용자 ID
        // JWT 토큰을 생성 (userId, 역할은 ROLE_USER로 설정)
        String token = jwtProvider.create(userId, "ROLE_USER");

        // 인증된 클라이언트(카카오, 네이버 등)에서 해당 인증 토큰 정보를 가져옴
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        // 리다이렉트 URL을 설정, 토큰과 만료 시간을 포함
        String redirectUrl = "http://localhost:3000/auth/oauth-response?" +
                "token=" + token +
                "&expires_in=" + client.getAccessToken().getExpiresAt().toEpochMilli();

        // refreshToken 추가: UserEntity를 조회하여 refreshToken 생성 후 저장
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            // refreshToken 생성
            String refreshToken = jwtProvider.createRefreshToken(userId);
            // 저장: JwtProvider 내부의 saveRefreshToken 메서드 호출
            jwtProvider.saveRefreshToken(user, refreshToken);
            // 리다이렉트 URL에 refreshToken 추가
            redirectUrl += "&refreshToken=" + refreshToken;
        } else {
            log.warn("UserEntity not found for userId: {}", userId);
        }


        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        // 카카오 로그인일 경우, 카카오 액세스 토큰 및 만료 시간 추가
        if ("kakao".equals(registrationId)) {
            OAuth2AuthorizedClient kakaoClient = authorizedClientService.loadAuthorizedClient(
                    "kakao", oauthToken.getName()
            );

            String kakaoAccessToken = kakaoClient.getAccessToken().getTokenValue();
            long kakaoExpiresIn = kakaoClient.getAccessToken().getExpiresAt().toEpochMilli();
            redirectUrl += "&kakaoToken=" + kakaoAccessToken + "&kakaoExpiresIn=" + kakaoExpiresIn;
        }

        // 네이버 로그인일 경우, 네이버 액세스 토큰 및 만료 시간 추가
        else if ("naver".equals(registrationId)) {
            OAuth2AuthorizedClient naverClient = authorizedClientService.loadAuthorizedClient(
                    "naver", oauthToken.getName()
            );

            String naverAccessToken = naverClient.getAccessToken().getTokenValue();
            long naverExpiresIn = naverClient.getAccessToken().getExpiresAt().toEpochMilli();
            redirectUrl += "&naverToken=" + naverAccessToken + "&naverExpiresIn=" + naverExpiresIn;
        }

        // 최종적으로 클라이언트(프론트엔드)로 리다이렉트
        response.sendRedirect(redirectUrl);
    }
}
