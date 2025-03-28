package kakao.login.provider;

import kakao.login.repository.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TokenCleanupTask {

    private final RefreshTokenRepository refreshTokenRepository;

    // 매일 새벽 2시에 만료된 토큰 삭제
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens();
    }

    // 매일 새벽 3시에 폐기된 토큰 삭제
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupRevokedTokens() {
        refreshTokenRepository.deleteRevokedTokens();
    }
}
