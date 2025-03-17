package kakao.login.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kakao.login.entity.UserEntity;
import kakao.login.provider.JwtProvider;
import kakao.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component  // 이 클래스는 Spring Security 필터로 등록됩니다.
@RequiredArgsConstructor  // 생성자 주입을 위한 Lombok 어노테이션
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;  // JWT 인증을 처리하는 JwtProvider
    private final UserRepository userRepository;  // 사용자 정보를 조회할 UserRepository

    // HTTP 요청마다 실행되는 필터 메서드
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try{
            // Authorization 헤더에서 JWT 토큰을 추출
            String token = parseBearerToken(request);
            if(token == null) {  // 토큰이 없으면 필터 체인 계속 진행
                filterChain.doFilter(request, response);
                return;
            }

            // JWT 토큰을 검증하고 userId를 추출
            String userId = jwtProvider.validate(token);
            if(userId == null){  // 유효하지 않은 토큰이면 필터 체인 계속 진행
                filterChain.doFilter(request, response);
                return;
            }

            // userId를 기반으로 UserEntity를 조회하여 역할(role) 정보 추출
            UserEntity userEntity = userRepository.findByUserId(userId);
            String role = userEntity.getRole();  // 역할 정보

            // 권한을 리스트로 저장 (여기서는 "ROLE_USER" 또는 "ROLE_ADMIN")
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(role));

            // 새롭게 생성된 인증 토큰 객체
            AbstractAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));  // HTTP 요청 정보 설정

            // SecurityContext에 인증 정보를 설정하여 인증된 상태로 만듬
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authenticationToken);  // 인증 정보 설정
            SecurityContextHolder.setContext(securityContext);  // SecurityContext에 설정

        } catch (Exception exception) {
            exception.printStackTrace();  // 예외 발생 시 스택 트레이스를 출력
        }

        // 필터 체인의 다음 필터로 요청을 전달
        filterChain.doFilter(request, response);
    }

    // Authorization 헤더에서 Bearer 토큰을 추출하는 메서드
    private String parseBearerToken(HttpServletRequest request){
        String authorization = request.getHeader("Authorization");  // Authorization 헤더 가져오기
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {  // Bearer 토큰이 아닐 경우 null 반환
            return null;
        }
        String token = authorization.substring(7).trim();  // "Bearer "를 제거하고 토큰만 추출
        // JWT 형식(두 개의 점 포함)인지 확인
        if (token.chars().filter(ch -> ch == '.').count() != 2) {
            return null;  // JWT 형식이 아니면 null 반환
        }
        return token;
    }
}
