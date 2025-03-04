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

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try{

            String token = parseBearerToken(request);
            if(token == null) {
                filterChain.doFilter(request,response);
                return;
            }

            String userId = jwtProvider.validate(token);
            if(userId == null){
                filterChain.doFilter(request,response);
                return;
            }

            UserEntity userEntity = userRepository.findByUserId(userId);
            String role = userEntity.getRole(); //role : ROLE_USRE, ROLE_ADMIN

            //ROLE_DEVELOPER, ROLE_BOSS 규칙 역할을 리스트에 저장
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(role));

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            AbstractAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            securityContext.setAuthentication(authenticationToken);
            SecurityContextHolder.setContext(securityContext);

        }catch (Exception exception){
            exception.printStackTrace();
        }

        filterChain.doFilter(request,response);

    }

    private String parseBearerToken(HttpServletRequest request){

        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        // 토큰이 JWT 형식(두 개의 점 포함)인지 확인
        if (token.chars().filter(ch -> ch == '.').count() != 2) {
            // JWT 형식이 아니라면 null을 반환하여 검증을 건너뜁니다.
            return null;
        }
        return token;
    }
}
