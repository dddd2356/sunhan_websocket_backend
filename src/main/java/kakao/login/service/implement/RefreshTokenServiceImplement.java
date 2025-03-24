package kakao.login.service.implement;

import kakao.login.entity.RefreshTokenEntity;
import kakao.login.entity.UserEntity;
import kakao.login.provider.JwtProvider;
import kakao.login.repository.RefreshTokenRepository;
import kakao.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service

public class RefreshTokenServiceImplement {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository; // UserRepository 추가
    private final JwtProvider jwtProvider;


    // Set expiry time (e.g., 7 days)
    private final long refreshTokenValidityInMilliseconds = 7 * 24 * 60 * 60 * 1000; // 7 days

    public RefreshTokenServiceImplement(RefreshTokenRepository refreshTokenRepository,
                                        UserRepository userRepository,
                                        JwtProvider jwtProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public RefreshTokenEntity createRefreshToken(String userId) {
        // Find the user
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // Revoke existing token if present
        refreshTokenRepository.findByUser(user).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });

        // Create new refresh token
        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setId(UUID.randomUUID().toString());
        refreshToken.setUser(user);

        // Generate token string using JwtProvider
        String tokenString = jwtProvider.createRefreshToken(userId);
        refreshToken.setToken(tokenString);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenValidityInMilliseconds));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshTokenEntity> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public Optional<RefreshTokenEntity> findByUserId(String userId) {
        return refreshTokenRepository.findByUserId(userId);
    }

    @Transactional
    public void revokeTokensByUserId(String userId) {
        refreshTokenRepository.revokeTokensByUserId(userId);
    }

    @Transactional
    public void revokeTokensByUser(UserEntity user)  {
        refreshTokenRepository.revokeTokensByUser(user);
    }

    public boolean verifyExpiration(RefreshTokenEntity token) {
        if (token.isExpired() || token.isRevoked()) {
            refreshTokenRepository.delete(token);
            return false;
        }
        return true;
    }

    @Transactional
    public Optional<String> refreshAccessToken(String refreshToken) {
        return findByToken(refreshToken)
                .filter(this::verifyExpiration)
                .map(token -> {
                    String userId = token.getUser().getUserId();
                    String role = token.getUser().getRole();
                    return jwtProvider.create(userId, role);
                });
    }

    // Logout method that revokes the token
    @Transactional
    public void logout(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            refreshTokenRepository.findByUser(user)
                    .ifPresent(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                    });
        });
    }
}
