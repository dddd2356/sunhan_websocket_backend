package kakao.login.repository;

import kakao.login.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {
    Optional<DepartmentEntity> findByDepartmentName(String name); // 이름으로 부서 조회

    Optional<DepartmentEntity> findById(Long id); // 이름으로 부서 조회


    // 모든 부서 이름을 조회하는 메서드
    @Query("SELECT d.departmentName FROM DepartmentEntity d")
    List<String> findAllDepartmentNames();

    // flag가 "delete"가 아닌 항목만 조회
    List<DepartmentEntity> findByFlagNot(String flag);

}
