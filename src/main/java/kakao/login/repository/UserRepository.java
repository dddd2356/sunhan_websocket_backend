package kakao.login.repository;

import kakao.login.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// 사용자 관련 DB 처리 리포지토리
@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    // userId로 사용자 조회
    UserEntity findByUserId(String userId);

    // userId가 존재하는지 여부 체크
    boolean existsByUserId(String userId);

    // 모든 사용자 조회
    List<UserEntity> findAll();

    // kakaoUuid를 업데이트하는 메소드
    @Modifying
    @Transactional
    @Query("UPDATE user u SET u.kakaoUuid = :kakaoUuid WHERE u.userId = CONCAT('kakao_', :rawUserId)")
    void updateKakaoUuid(@Param("kakaoUuid") String kakaoUuid, @Param("rawUserId") String rawUserId);
}
