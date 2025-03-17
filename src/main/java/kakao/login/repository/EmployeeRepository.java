package kakao.login.repository;

import kakao.login.entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 직원 관련 DB 처리 리포지토리
@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {

    // userId로 직원 조회
    EmployeeEntity findByUser_UserId(String userId);

    // 특정 부서의 직원들 조회
    List<EmployeeEntity> findByDepartment_DepartmentName(String departmentName);

    // 여러 부서의 직원 조회
    List<EmployeeEntity> findByDepartment_DepartmentNameIn(List<String> departmentNames);

    // 특정 부서와 구역에 해당하는 직원들 조회
    List<EmployeeEntity> findByDepartment_DepartmentNameAndSection_SectionName(String departmentName, String sectionName);

    // 모든 직원의 userId 조회
    @Query("SELECT e.user.userId FROM EmployeeEntity e")
    List<String> findAllUserIds();

    // 'kakao' 타입인 직원들의 userId 조회
    @Query("SELECT e.user.userId FROM EmployeeEntity e WHERE e.user.type = 'kakao'")
    List<String> findAllUserIdsWithKakao();

    // 여러 직원들의 userId로 조회
    List<EmployeeEntity> findByUser_UserIdIn(List<String> userIds);

    // 직원 이름에 해당하는 부분 조회
    List<EmployeeEntity> findByNameContaining(String keyword);

    // 부서 이름과 직원 이름으로 조회
    List<EmployeeEntity> findByNameContainingAndDepartment_DepartmentName(String keyword, String departmentName);

    // userId로 직원 조회 (Fetch Join)
    @Query("SELECT e FROM EmployeeEntity e LEFT JOIN FETCH e.user u WHERE u.userId = :userId")
    Optional<EmployeeEntity> findEmployeeWithUser(@Param("userId") String userId);

    // 특정 직원들의 카카오 UUID 목록 조회
    @Query("SELECT e.user.userId FROM EmployeeEntity e WHERE e.user.userId LIKE 'kakao_%' AND e.user.userId IN :userIds")
    List<String> findKakaoUuidsWithEmployees(@Param("userIds") List<String> userIds);

    // 카카오 UUID로 직원 조회
    List<EmployeeEntity> findByKakaoUuidIn(List<String> kakaoUuids);
}
