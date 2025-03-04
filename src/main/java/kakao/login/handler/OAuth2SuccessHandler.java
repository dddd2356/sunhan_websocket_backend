package kakao.login.handler;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kakao.login.entity.CustomOAuth2User;
import kakao.login.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    //토큰 생성
    private final JwtProvider jwtProvider;
    //카카오 토큰 따로 생성
    private final OAuth2AuthorizedClientService authorizedClientService;

    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        // 인증 토큰 캐스팅
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        String userId = oAuth2User.getName();
        //토큰생성
        String token = jwtProvider.create(userId);

// 등록된 클라이언트(카카오, 네이버 등)에서 해당 토큰 정보를 가져오기
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        String redirectUrl = "http://localhost:3000/auth/oauth-response?" +
                "token=" + token +
                "&expires_in=" +  client.getAccessToken().getExpiresAt().toEpochMilli(); ; // 기본적으로 공통 파라미터


        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        if ("kakao".equals(registrationId)) {
            // 카카오 토큰 가져오기
            OAuth2AuthorizedClient kakaoClient = authorizedClientService.loadAuthorizedClient(
                    "kakao", // 카카오 로그인 등록 ID
                    oauthToken.getName()
            );

            String kakaoAccessToken = kakaoClient.getAccessToken().getTokenValue();
            long kakaoExpiresIn = kakaoClient.getAccessToken().getExpiresAt().toEpochMilli();

            // 카카오 토큰만 전달
            redirectUrl += "&kakaoToken=" + kakaoAccessToken + "&kakaoExpiresIn=" + kakaoExpiresIn;
        }

        else if ("naver".equals(registrationId)) {
            // 네이버 토큰 가져오기
            OAuth2AuthorizedClient naverClient = authorizedClientService.loadAuthorizedClient(
                    "naver", // 네이버 로그인 등록 ID
                    oauthToken.getName()
            );

            String naverAccessToken = naverClient.getAccessToken().getTokenValue();
            long naverExpiresIn = naverClient.getAccessToken().getExpiresAt().toEpochMilli();
            // 네이버 토큰만 전달
            redirectUrl += "&naverToken=" + naverAccessToken + "&naverExpiresIn=" + naverExpiresIn;
        }

        // 리다이렉트
        response.sendRedirect(redirectUrl);

    }

}
