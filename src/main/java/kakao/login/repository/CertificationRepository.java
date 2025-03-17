package kakao.login.repository;

import jakarta.transaction.Transactional;
import kakao.login.entity.CertificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// 인증 관련 DB 처리 리포지토리
@Repository
public interface CertificationRepository extends JpaRepository<CertificationEntity, String> {

    // userId로 인증 정보를 조회하는 메소드
    CertificationEntity findByUserId(String userId);

    // 특정 userId에 대한 인증 데이터를 삭제하는 메소드
    @Transactional
    void deleteByUserId(String userId);
}
