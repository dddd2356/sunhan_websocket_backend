package kakao.login.repository;

import kakao.login.entity.CertificationEntity;
import kakao.login.entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {
    EmployeeEntity findByUser_UserId(String userId);  // UserEntity의 userId로 조회

    // 특정 부서의 직원들 조회
    List<EmployeeEntity> findByDepartment_DepartmentName(String departmentName);

    // 특정 부서와 구역에 해당하는 직원들 조회
    List<EmployeeEntity> findByDepartment_DepartmentNameAndSection_SectionName(String departmentName, String sectionName);
}
