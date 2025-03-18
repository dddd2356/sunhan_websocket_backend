package kakao.login.repository;
import kakao.login.entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {
    EmployeeEntity findByUser_UserId(String userId);  // UserEntity의 userId로 조회

    // 특정 부서의 직원들 조회
    List<EmployeeEntity> findByDepartment_DepartmentName(String departmentName);

    // 여러 부서의 직원 조회 (여러 개 부서 선택 가능)
    List<EmployeeEntity> findByDepartment_DepartmentNameIn(List<String> departmentNames);

    // 특정 부서와 구역에 해당하는 직원들 조회
    List<EmployeeEntity> findByDepartment_DepartmentNameAndSection_SectionName(String departmentName, String sectionName);

    // 직원으로 등록된 모든 유저 ID 조회 (네이티브 쿼리 사용)
    @Query("SELECT e.user.userId FROM EmployeeEntity e")
    List<String> findAllUserIds();

    // type이 'kakao'인 직원들의 userId 조회
    @Query("SELECT e.user.userId FROM EmployeeEntity e WHERE e.user.type = 'kakao'")
    List<String> findAllUserIdsWithKakao();  // 'kakao'로 로그인한 유저의 userId만 조회

    List<EmployeeEntity> findByUser_UserIdIn(List<String> userIds); // UserEntity의 userId는 String 타입

    List<EmployeeEntity> findByNameContaining(String keyword);
    List<EmployeeEntity> findByNameContainingAndDepartment_DepartmentName(String keyword, String departmentName);

    @Query("SELECT e FROM EmployeeEntity e LEFT JOIN FETCH e.user u WHERE u.userId = :userId")
    Optional<EmployeeEntity> findEmployeeWithUser(@Param("userId") String userId);
    // 추가된 부분: 카카오 UUID 목록 조회
    @Query("SELECT e.user.userId FROM EmployeeEntity e WHERE e.user.userId LIKE 'kakao_%' AND e.user.userId IN :userIds")
    List<String> findKakaoUuidsWithEmployees(@Param("userIds") List<String> userIds); // 특정 직원들의 카카오 UUID 목록 조회

    List<EmployeeEntity> findByKakaoUuidIn(List<String> kakaoUuids);
}