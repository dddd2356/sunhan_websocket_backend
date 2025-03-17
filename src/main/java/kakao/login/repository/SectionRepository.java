package kakao.login.repository;

import kakao.login.entity.DepartmentEntity;
import kakao.login.entity.SectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 구역 관련 DB 처리 리포지토리
@Repository
public interface SectionRepository extends JpaRepository<SectionEntity, Long> {

    // 섹션 이름으로 구역 조회
    Optional<SectionEntity> findBySectionName(String name);

    // 특정 부서에 해당하는 구역 목록을 조회
    @Query("SELECT s.sectionName FROM SectionEntity s WHERE s.department.departmentName = :department")
    List<String> findSectionsByDepartment(@Param("department") String department);

    // 부서와 구역 이름을 기준으로 구역 조회
    Optional<SectionEntity> findBySectionNameAndDepartment(String sectionName, DepartmentEntity department);
}
