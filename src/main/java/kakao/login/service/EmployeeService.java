package kakao.login.service;

import jakarta.transaction.Transactional;
import kakao.login.entity.DepartmentEntity;
import kakao.login.entity.SectionEntity;
import kakao.login.entity.UserEntity;
import kakao.login.entity.EmployeeEntity;
import kakao.login.repository.DepartmentRepository;
import kakao.login.repository.EmployeeRepository;
import kakao.login.repository.SectionRepository;
import kakao.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository, UserRepository userRepository, DepartmentRepository departmentRepository, SectionRepository sectionRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
    }

    // 직원 등록 메소드
    // 직원 등록 메소드
    @Transactional
    public EmployeeEntity registerEmployee(String userId, String name, String phone, String departmentName, String sectionName, String position, MultipartFile profileImage) throws IOException {
        // userId를 기반으로 기존 회원 찾기
        UserEntity user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // 부서 이름으로 부서 조회
        DepartmentEntity department = departmentRepository.findByDepartmentName(departmentName)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        // 섹션 이름으로 섹션 조회
        SectionEntity section = sectionRepository.findBySectionName(sectionName)
                .orElseThrow(() -> new RuntimeException("Section not found"));

        // 기존에 EmployeeEntity가 등록되어 있는지 확인 (필요시 findByUser_Id 사용)
        EmployeeEntity existingEmployee = employeeRepository.findByUser_UserId(userId);
        if (existingEmployee != null) {
            throw new RuntimeException("Employee already registered");
        }

        // EmployeeEntity 객체 생성
        EmployeeEntity employee = new EmployeeEntity();
        employee.setUser(user); // user_id 값 설정
        employee.setName(name);
        employee.setPhone(phone);
        employee.setDepartment(department);
        employee.setSection(section);
        employee.setPosition(position);

        // 프로필 이미지가 있을 경우, byte[]로 변환하여 설정
        if (profileImage != null && !profileImage.isEmpty()) {
            employee.setProfileImage(profileImage.getBytes());
        }

        // 직원 저장 후 반환
        return employeeRepository.save(employee);
    }


    // 직원 정보 조회 메소드
    public EmployeeEntity getEmployeeInfo(String userId) {
        EmployeeEntity employee = employeeRepository.findByUser_UserId(userId);

        if (employee != null && employee.getProfileImage() != null) {
            // 프로필 이미지가 있을 경우 Base64로 변환하여 set
            String profileImageBase64 = Base64.getEncoder().encodeToString(employee.getProfileImage());
            employee.setProfileImageBase64(profileImageBase64);
        }

        return employee;
    }

    public List<EmployeeEntity> getAllEmployees() {
        return employeeRepository.findAll(); // 전체 직원 목록 조회
    }

    // 부서별 직원 조회
    public List<EmployeeEntity> getEmployeesByDepartment(String department) {
        return employeeRepository.findByDepartment_DepartmentName(department); // 부서 이름으로 직원 조회
    }

    // 부서 및 구역별 직원 조회
    public List<EmployeeEntity> getEmployeesByDepartmentAndSection(String department, String section) {
        return employeeRepository.findByDepartment_DepartmentNameAndSection_SectionName(department, section); // 부서와 섹션 이름으로 직원 조회
    }
}