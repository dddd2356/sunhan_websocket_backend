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
import java.util.*;

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

        // 기존 토큰들을 모두 조회한 후 폐기 처리
        List<RefreshTokenEntity> tokens = refreshTokenRepository.findByUser(user);
        if (!tokens.isEmpty()) {
            tokens.forEach(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }

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
    public Optional<Map<String, Object>> refreshAccessToken(String refreshToken) {
        Optional<RefreshTokenEntity> tokenOpt = findByToken(refreshToken);
        if (tokenOpt.isEmpty() || !verifyExpiration(tokenOpt.get())) {
            return Optional.empty();
        }
        RefreshTokenEntity tokenEntity = tokenOpt.get();

        // refresh 토큰 사용 후 폐기 처리
        tokenEntity.setRevoked(true);
        refreshTokenRepository.save(tokenEntity);

        // 새로운 access 토큰 생성
        String userId = tokenEntity.getUser().getUserId();
        String role = tokenEntity.getUser().getRole();
        String newAccessToken = jwtProvider.create(userId, role);

        // 회전 방식: 새 refresh 토큰 생성
        RefreshTokenEntity newRefreshTokenEntity = createRefreshToken(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("refreshToken", newRefreshTokenEntity.getToken());

        return Optional.of(result);
    }


    // Logout method that revokes the token
    @Transactional
    public void logout(String userId, String refreshToken) {
        Optional<UserEntity> userOptional = userRepository.findById(userId);

        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();

            // 해당 토큰을 찾아서 폐기
            Optional<RefreshTokenEntity> tokenOptional = refreshTokenRepository.findByToken(refreshToken);
            tokenOptional.ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });

            // 해당 사용자의 모든 토큰 폐기
            List<RefreshTokenEntity> userTokens = refreshTokenRepository.findByUser(user);
            userTokens.forEach(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }
}
