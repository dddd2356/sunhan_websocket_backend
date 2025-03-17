package kakao.login.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtProvider {

    @Value("${secret-key}")  // application.yml에서 정의된 secret-key를 가져옴
    private String secretKey;

    // JWT 생성 메서드 (사용자 ID와 역할을 기반으로 토큰 생성)
    public String create(String userId, String role){

        // JWT Claims에 사용자 ID와 역할을 설정
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("role", role);  // 역할을 Claims에 추가

        // 토큰 만료 기간 설정 (1시간 후)
        Date expiredDate = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));  // 비밀 키를 사용하여 서명

        // JWT 생성
        String jwt = Jwts.builder()
                .signWith(key, SignatureAlgorithm.HS256)  // HS256 알고리즘으로 서명
                .setClaims(claims)
                .setSubject(userId)  // 토큰의 subject는 사용자 ID
                .setIssuedAt(new Date())  // 토큰 발급 시점
                .setExpiration(expiredDate)  // 만료 일자 설정
                .compact();  // JWT 토큰 반환

        return jwt;
    }

    // JWT 검증 메서드 (JWT에서 사용자 ID 추출)
    public String validate(String jwt) {

        String subject = null;
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));  // 비밀 키 생성

        try{
            // JWT를 파싱하여 Claims에서 사용자 ID 추출
            subject = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody()
                    .getSubject();
        }catch (Exception exception){
            exception.printStackTrace();
            return null;  // 검증 실패 시 null 반환
        }
        return subject;  // 검증 성공 시 사용자 ID 반환
    }
}
