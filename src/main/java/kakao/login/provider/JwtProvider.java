package kakao.login.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class JwtProvider {

    @Value("${secret-key}")
    private String secretKey;

    @Value("${refresh-key:${secret-key}}")
    private String refreshKey;

    @Value("${jwt.access-token.expiration:3600000}") // Default: 1 hour in milliseconds
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration:1209600000}") // Default: 14 days in milliseconds
    private long refreshTokenExpiration;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private Key signingKey;
    private Key refreshSigningKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.refreshSigningKey = Keys.hmacShaKeyFor(refreshKey.getBytes(StandardCharsets.UTF_8));
        log.info("Access Token Expiration: {}ms", accessTokenExpiration);
        log.info("Refresh Token Expiration: {}ms", refreshTokenExpiration);
    }

    public String create(String userId, String role) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("role", role);
        claims.put("type", "access");

        Date expiredDate = Date.from(Instant.now().plus(accessTokenExpiration, ChronoUnit.MILLIS));
        log.info("Creating access token for userId: {}, expires at: {}", userId, expiredDate);
        return Jwts.builder()
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(expiredDate)
                .compact();
    }



    public String createRefreshToken(String userId) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("type", "refresh");

        Date expiredDate = Date.from(Instant.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS));
        return Jwts.builder()
                .signWith(refreshSigningKey, SignatureAlgorithm.HS256)
                .setClaims(claims)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(expiredDate)
                .compact();
    }

    public String validate(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .setAllowedClockSkewSeconds(30) // Consistent 30-second skew
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            log.error("Access token expired: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Invalid access token: {}", e.getMessage());
            return null;
        }
    }

    public Optional<String> validateRefreshToken(String refreshToken) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(refreshSigningKey)
                    .setAllowedClockSkewSeconds(30)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();

            String tokenType = claims.get("type", String.class);
            if (!"refresh".equals(tokenType)) {
                log.error("Invalid token type: {}", tokenType);
                return Optional.empty();
            }

            return Optional.ofNullable(claims.getSubject());
        } catch (ExpiredJwtException e) {
            log.error("Refresh token expired: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Invalid refresh token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .setAllowedClockSkewSeconds(30)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("Token expired: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String getTokenIdFromRefreshToken(String refreshToken) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(refreshSigningKey)
                    .setAllowedClockSkewSeconds(30)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();
            return claims.getId();
        } catch (Exception e) {
            log.error("Failed to extract token ID: {}", e.getMessage());
            return null;
        }
    }

    public boolean verifyRefreshToken(String refreshToken) {
        String tokenId = getTokenIdFromRefreshToken(refreshToken);
        if (tokenId == null) {
            log.error("No token ID extracted from refresh token");
            return false;
        }

        Optional<RefreshTokenEntity> tokenOpt = refreshTokenRepository.findByToken(refreshToken);
        if (tokenOpt.isEmpty()) {
            log.error("Refresh token not found in database");
            return false;
        }

        RefreshTokenEntity tokenEntity = tokenOpt.get();
        log.info("Token details - User: {}, Revoked: {}, Expired: {}",
                tokenEntity.getUser().getUserId(),
                tokenEntity.isRevoked(),
                tokenEntity.isExpired());

        // Allow token refresh even if previous token was revoked
        if (tokenEntity.isExpired()) {
            log.warn("Refresh token is expired");
            return false;
        }

        return true;
    }

    public Optional<String> refreshAccessToken(String refreshToken) {
        if (!verifyRefreshToken(refreshToken)) {
            log.error("Refresh token verification failed");
            return Optional.empty();
        }

        String userId = validateRefreshToken(refreshToken).orElse(null);
        if (userId == null) {
            log.error("No user ID found in refresh token");
            return Optional.empty();
        }

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("User not found: {}", userId);
            return Optional.empty();
        }

        String role = userOpt.get().getRole();
        return Optional.of(create(userId, role));
    }

    public void saveRefreshToken(UserEntity user, String refreshToken) {
        String tokenId = getTokenIdFromRefreshToken(refreshToken);
        if (tokenId == null) {
            log.error("Failed to extract token ID from refresh token");
            log.error("Full refresh token: {}", refreshToken);
            return;
        }

        // Log all existing tokens before saving
        List<RefreshTokenEntity> existingTokens = refreshTokenRepository.findByUser(user);
        for (RefreshTokenEntity existingToken : existingTokens) {
            // Instead of deleting, just mark as revoked
            existingToken.setRevoked(true);
            refreshTokenRepository.save(existingToken);
        }
        log.info("Existing tokens for user {}: {}", user.getUserId(), existingTokens.size());

        RefreshTokenEntity tokenEntity = new RefreshTokenEntity();
        tokenEntity.setId(tokenId);
        tokenEntity.setUser(user);
        tokenEntity.setToken(refreshToken);
        tokenEntity.setExpiryDate(Instant.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS));
        tokenEntity.setRevoked(false);

        refreshTokenRepository.save(tokenEntity);
        log.info("Refresh token saved for user: {}, tokenId: {}", user.getUserId(), tokenId);
    }


    public void revokeRefreshToken(String refreshToken) {
        Optional<RefreshTokenEntity> tokenOpt = refreshTokenRepository.findByToken(refreshToken);
        tokenOpt.ifPresent(token -> {
            if (!token.isExpired()) {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                log.info("Refresh token revoked: {}", refreshToken); // 디버깅 로그 추가
            } else {
                log.warn("Attempted to revoke already expired token: {}", refreshToken);
            }
        });
        if (tokenOpt.isEmpty()) {
            log.warn("Refresh token not found in repository: {}", refreshToken);
        }
    }

    public void revokeAllUserTokens(String userId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        userOpt.ifPresent(user -> refreshTokenRepository.deleteByUser(user));
    }

    public long getAccessTokenExpirationTime() {
        long expiresInSeconds = accessTokenExpiration / 1000;
        log.info("getAccessTokenExpirationTime: {} seconds", expiresInSeconds);
        return expiresInSeconds; // 3600초 반환
    }

    public long getRefreshTokenExpirationTime() {
        return refreshTokenExpiration / 1000; // Convert to seconds
    }


}