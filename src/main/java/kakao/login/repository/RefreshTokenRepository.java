package kakao.login.repository;

import kakao.login.entity.RefreshTokenEntity;
import kakao.login.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {
    // UserEntity를 기준으로 리프레시 토큰을 찾는 메서드
    Optional<RefreshTokenEntity> findByToken(String token);

    Optional<RefreshTokenEntity> findByUser(UserEntity user);


    // UserEntity를 기준으로 리프레시 토큰을 취소하는 메서드
    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.user.userId = :userId")
    void revokeTokensByUserId(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.user = :user")
    void revokeTokensByUser(@Param("user") UserEntity user);

    // UserEntity를 기준으로 리프레시 토큰을 삭제하는 메서드
    void deleteByUser(UserEntity user);

    boolean existsByUser(UserEntity user);

    // New method to find by userId
    @Query("SELECT r FROM RefreshTokenEntity r WHERE r.user.userId = :userId")
    Optional<RefreshTokenEntity> findByUserId(@Param("userId") String userId);

}