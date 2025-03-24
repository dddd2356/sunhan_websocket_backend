package kakao.login.repository;
import kakao.login.entity.SectionSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SectionSettingsRepository extends JpaRepository<SectionSettings, Long> {
    // 첫 번째 설정을 가져오기 위한 메소드
    SectionSettings findFirstByOrderById();
}