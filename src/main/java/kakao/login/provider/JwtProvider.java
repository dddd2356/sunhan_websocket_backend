package kakao.login.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import kakao.login.entity.RefreshTokenEntity;
import kakao.login.entity.UserEntity;
import kakao.login.repository.RefreshTokenRepository;
import kakao.login.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class JwtProvider {

    @Value("${secret-key}")  // application.yml에서 정의된 secret-key를 가져옴
    private String secretKey;

    @Value("${refresh-key:${secret-key}}")  // refresh 토큰용 시크릿 키 (기본값은 secret-key)
    private String refreshKey;

    // 액세스 토큰 만료 시간 (기본값 1시간)
    @Value("${access-token.expiration:1}")
    private long accessTokenExpiration;

    // 리프레시 토큰 만료 시간 (기본값 14일)
    @Value("${refresh-token.expiration:14}")
    private long refreshTokenExpiration;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository; // Add this field

    // 🔽 Key 객체를 필드로 선언하여 여러 메서드에서 재사용
    private Key signingKey;
    private Key refreshSigningKey;

    // 액세스 토큰 만료 시간 (초 단위)
    private final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 30; // 30분

    // 리프레시 토큰 만료 시간 (초 단위)
    private final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24 * 14; // 14일


    // ✅ @PostConstruct를 활용한 키 초기화
    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.refreshSigningKey = Keys.hmacShaKeyFor(refreshKey.getBytes(StandardCharsets.UTF_8));
    }



    // 액세스 토큰 생성 메서드 (사용자 ID와 역할을 기반으로 토큰 생성)
    public String create(String userId, String role) {
        // JWT Claims에 사용자 ID와 역할을 설정
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("role", role);  // 역할을 Claims에 추가
        claims.put("type", "access");  // 토큰 타입 지정

        // 토큰 만료 기간 설정 (설정된 시간 후)
        Date expiredDate = Date.from(Instant.now().plus(accessTokenExpiration, ChronoUnit.HOURS));
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

    // 리프레시 토큰 생성 메서드
    public String createRefreshToken(String userId) {
        // 리프레시 토큰용 Claims 설정
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("type", "refresh");  // 토큰 타입 지정

        // 리프레시 토큰은 더 긴 유효기간을 가짐 (설정된 일수)
        Date expiredDate = Date.from(Instant.now().plus(refreshTokenExpiration, ChronoUnit.DAYS));
        Key key = Keys.hmacShaKeyFor(refreshKey.getBytes(StandardCharsets.UTF_8));

        // 토큰 ID 생성 (관리 및 무효화를 위한 식별자)
        String tokenId = UUID.randomUUID().toString();

        // 리프레시 토큰 생성
        String refreshToken = Jwts.builder()
                .signWith(key, SignatureAlgorithm.HS256)
                .setClaims(claims)
                .setId(tokenId)  // 토큰 ID 설정
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(expiredDate)
                .compact();

        return refreshToken;
    }

    // 액세스 토큰 검증 메서드
    // 토큰 검증 및 사용자 ID 추출
    public String validate(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)  // secretKey 대신 signingKey 사용
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    // 액세스 토큰 만료 시간 반환 (초 단위)
    public long getAccessTokenExpirationTime() {
        return ACCESS_TOKEN_EXPIRATION / 1000;
    }

    // 리프레시 토큰 만료 시간 반환 (초 단위)
    public long getRefreshTokenExpirationTime() {
        return REFRESH_TOKEN_EXPIRATION / 1000;
    }

    public Optional<String> validateRefreshToken(String refreshToken) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(refreshToken)
                    .getBody();

            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid refresh token: {}", e.getMessage());
            return Optional.empty();
        }
    }
    // 토큰 검증을 위한 공통 메서드
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    // 리프레시 토큰에서 토큰 ID 추출
    public String getTokenIdFromRefreshToken(String refreshToken) {
        try {
            Key key = Keys.hmacShaKeyFor(refreshKey.getBytes(StandardCharsets.UTF_8));

            // JWT 파싱
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();

            return claims.getId();  // 토큰 ID 반환
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    // 사용자 ID와 역할로 새 액세스 토큰 발급
    public Optional<String> refreshAccessToken(String refreshToken) {
        if (!verifyRefreshToken(refreshToken)) {
            return Optional.empty();
        }

        String userId = validateRefreshToken(refreshToken).orElse(null);
        if (userId == null) {
            return Optional.empty();
        }

        // 사용자 역할을 불러오는 로직 필요
        String role = "USER"; // 예시: 실제로는 DB에서 조회 필요

        return Optional.of(create(userId, role));
    }


    // 기존 메서드 일부 수정 예시
// 리프레시 토큰 저장 메서드
    public void saveRefreshToken(UserEntity user, String refreshToken) {
        String tokenId = getTokenIdFromRefreshToken(refreshToken);
        if (tokenId == null) {
            System.out.println("토큰 ID를 추출하지 못했습니다.");
            return;
        }

        // 기존 토큰이 있으면 삭제
        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);

        // 새 토큰 저장
        RefreshTokenEntity tokenEntity = new RefreshTokenEntity();
        tokenEntity.setId(tokenId);
        tokenEntity.setUser(user); // UserEntity 저장
        tokenEntity.setToken(refreshToken);
        tokenEntity.setExpiryDate(Instant.now().plus(refreshTokenExpiration, ChronoUnit.DAYS));
        tokenEntity.setRevoked(false);

        refreshTokenRepository.save(tokenEntity);
        System.out.println("새 refresh token 저장됨: " + refreshToken);
    }


    // 리프레시 토큰 검증 (데이터베이스 기반)
    public boolean verifyRefreshToken(String refreshToken) {
        // 토큰 ID 추출
        String tokenId = getTokenIdFromRefreshToken(refreshToken);
        if (tokenId == null) return false;

        // 데이터베이스에서 토큰 조회
        Optional<RefreshTokenEntity> tokenOpt = refreshTokenRepository.findByToken(refreshToken);
        if (tokenOpt.isEmpty()) return false;

        // Optional에서 값을 꺼내서 토큰 검증
        RefreshTokenEntity tokenEntity = tokenOpt.get();

        // 토큰 상태 확인
        return !tokenEntity.isRevoked() && !tokenEntity.isExpired();
    }


    // 리프레시 토큰 폐기
    public void revokeRefreshToken(String refreshToken) {
        Optional<RefreshTokenEntity> tokenOpt = refreshTokenRepository.findByToken(refreshToken);
        tokenOpt.ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    // 모든 리프레시 토큰 폐기 (로그아웃 시)
    public void revokeAllUserTokens(String userId) {
        // UserEntity를 통해 삭제하도록 함
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        userOpt.ifPresent(user -> refreshTokenRepository.deleteByUser(user));
    }
}