package kakao.login.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kakao.login.filter.JwtAuthenticationFilter;
import kakao.login.handler.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;

@Configurable
@Configuration  // Spring 설정 클래스
@EnableWebSecurity  // Spring Security 활성화
@RequiredArgsConstructor  // 생성자 주입을 위한 Lombok 어노테이션
public class WebSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;  // JWT 인증 필터
    private final DefaultOAuth2UserService oAuth2UserService;  // OAuth2 사용자 정보 서비스
    private final OAuth2SuccessHandler oAuth2SuccessHandler;  // OAuth2 로그인 성공 후 처리 핸들러

    @Bean  // HTTP 보안 관련 설정을 구성하는 메서드
    protected SecurityFilterChain configure(HttpSecurity httpSecurity) throws Exception{
        httpSecurity
                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 보호 비활성화
                .csrf(CsrfConfigurer::disable)

                // HTTP 기본 인증 비활성화
                .httpBasic(HttpBasicConfigurer::disable)

                // 세션 관리 설정 (Stateless 설정으로 세션을 사용하지 않음)
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL 별 접근 권한 설정
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/", "/api/v1/auth/**", "/oauth2/**", "/api/v1/naver/**", "/api/v1/user/**", "/api/v1/detail/**").permitAll()  // 누구나 접근 가능한 URL
                        .requestMatchers("/api/v1/user/**").hasRole("USER")  // USER 역할만 접근 가능
                        .requestMatchers("/api/v1/admin/**", "/api/v1/detail/department/**").hasRole("ADMIN")  // ADMIN 역할만 접근 가능
                        .requestMatchers("/api/v1/detail/employment/signup").hasRole("ADMIN")  // 관리자만 접근 가능
                        .anyRequest().authenticated()  // 나머지 요청은 인증된 사용자만 접근 가능
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint.baseUri("/api/v1/auth/oauth2"))  // 인증 요청의 엔드포인트 URI 설정
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/oauth2/callback/*"))  // 리다이렉션 엔드포인트 설정
                        .userInfoEndpoint(endpoint -> endpoint.userService(oAuth2UserService))  // 사용자 정보 서비스 설정
                        .successHandler(oAuth2SuccessHandler)  // 로그인 성공 핸들러 설정
                )

                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout/")  // 로그아웃 API URL 설정
                        .deleteCookies("accessToken", "kakaoAccessToken", "naverAccessToken", "refreshToken")  // 로그아웃 시 쿠키 삭제
                        .invalidateHttpSession(true)  // 세션 무효화
                )

                // 예외 처리 설정
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new FailedAuthenticationEntryPoint())  // 인증 실패 시 응답 설정
                )

                // JWT 인증 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean  // CORS 설정을 위한 메서드
    protected CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("http://localhost:3000");  // 허용된 도메인 설정
        corsConfiguration.addAllowedOrigin("http://localhost:4040");  // 추가 허용 도메인
        corsConfiguration.addAllowedMethod("*");  // 모든 HTTP 메서드 허용
        corsConfiguration.addAllowedHeader("*");  // 모든 HTTP 헤더 허용
        corsConfiguration.setAllowCredentials(true);  // 쿠키 전송 허용 (true)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", corsConfiguration);  // 특정 URL 경로에 CORS 설정 적용

        return source;
    }

    // 인증 실패 시 처리하는 클래스
    class FailedAuthenticationEntryPoint implements AuthenticationEntryPoint {
        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);  // 상태 코드 403 (Forbidden) 설정
            response.getWriter().write("{\"code\": \"NP\", \"message\": \"No Permission.\"}");  // 응답 메시지 설정
        }
    }
}
