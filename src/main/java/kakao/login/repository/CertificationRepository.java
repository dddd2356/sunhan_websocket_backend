package kakao.login.repository;


import jakarta.transaction.Transactional;
import kakao.login.entity.CertificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificationRepository extends JpaRepository<CertificationEntity, String> {
    CertificationEntity findByUserId(String userId);

    @Transactional
    void deleteByUserId(String userId);

}
