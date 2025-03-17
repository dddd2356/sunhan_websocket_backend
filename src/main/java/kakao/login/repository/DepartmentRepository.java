package kakao.login.repository;

import kakao.login.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 부서 관련 DB 처리 리포지토리
@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

    // 부서 이름으로 부서 조회
    Optional<DepartmentEntity> findByDepartmentName(String name);

    // 부서 ID로 부서 조회
    Optional<DepartmentEntity> findById(Long id);

    // 모든 부서 이름을 조회하는 쿼리
    @Query("SELECT d.departmentName FROM DepartmentEntity d")
    List<String> findAllDepartmentNames();

    // flag가 "delete"가 아닌 부서만 조회
    List<DepartmentEntity> findByFlagNot(String flag);
}
