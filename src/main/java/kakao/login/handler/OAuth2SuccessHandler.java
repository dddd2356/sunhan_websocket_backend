package kakao.login.handler;

import jakarta.servlet.ServletException;
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

    private final JwtProvider jwtProvider;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String userId = oAuth2User.getName();

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            String role = user.getRole(); // ✅ DB에서 role 가져오기
            String token = jwtProvider.create(userId, role); // ✅ 실제 role로 토큰 생성

            long expiresIn = jwtProvider.getAccessTokenExpirationTime();

            if (expiresIn <= 0) {
                log.error("Invalid expiresIn value: {}", expiresIn);
                expiresIn = 3600; // Fallback to 3600 seconds if invalid
            }

            String redirectUrl = "http://localhost:3000/auth/oauth-response?" +
                    "token=" + token +
                    "&expiresIn=" + expiresIn;


            if (userOpt.isPresent()) {
                // Always generate a new refresh token
                String refreshToken = jwtProvider.createRefreshToken(userId);
                jwtProvider.saveRefreshToken(user, refreshToken);

                redirectUrl += "&refreshToken=" + refreshToken;
                log.info("New refresh token generated for userId: {}", userId);
            }


            String registrationId = oauthToken.getAuthorizedClientRegistrationId();
            if ("kakao".equals(registrationId)) {
                OAuth2AuthorizedClient kakaoClient = authorizedClientService.loadAuthorizedClient("kakao", oauthToken.getName());
                String kakaoAccessToken = kakaoClient.getAccessToken().getTokenValue();
                long kakaoExpiresIn = java.time.Duration.between(
                        java.time.Instant.now(),
                        kakaoClient.getAccessToken().getExpiresAt()
                ).getSeconds();
                redirectUrl += "&kakaoToken=" + kakaoAccessToken + "&kakaoExpiresIn=" + kakaoExpiresIn;
            } else if ("naver".equals(registrationId)) {
                OAuth2AuthorizedClient naverClient = authorizedClientService.loadAuthorizedClient("naver", oauthToken.getName());
                String naverAccessToken = naverClient.getAccessToken().getTokenValue();
                long naverExpiresIn = java.time.Duration.between(
                        java.time.Instant.now(),
                        naverClient.getAccessToken().getExpiresAt()
                ).getSeconds();
                redirectUrl += "&naverToken=" + naverAccessToken + "&naverExpiresIn=" + naverExpiresIn;
            }

            log.info("Redirecting to: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
        }
    }
}