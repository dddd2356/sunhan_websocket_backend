package kakao.login.repository;

import kakao.login.entity.SectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<SectionEntity, Long> {
    // 필요한 추가적인 쿼리 메서드를 정의할 수 있습니다.
    Optional<SectionEntity> findBySectionName(String name); // 이름으로 섹션 조회
    // 특정 부서에 해당하는 구역 목록을 조회하는 메서드
    @Query("SELECT s.sectionName FROM SectionEntity s WHERE s.department.departmentName = :department")
    List<String> findSectionsByDepartment(@Param("department") String department);
}