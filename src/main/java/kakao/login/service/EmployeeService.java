package kakao.login.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import kakao.login.dto.request.message.EmployeeRequestDTO;
import kakao.login.entity.DepartmentEntity;
import kakao.login.entity.SectionEntity;
import kakao.login.entity.UserEntity;
import kakao.login.entity.EmployeeEntity;
import kakao.login.repository.DepartmentRepository;
import kakao.login.repository.EmployeeRepository;
import kakao.login.repository.SectionRepository;
import kakao.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
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

        // 섹션이 전달된 경우에만 섹션 조회 (섹션 사용이 선택사항인 경우)
        SectionEntity section = null;
        if (sectionName != null && !sectionName.trim().isEmpty()) {
            section = sectionRepository.findBySectionName(sectionName)
                    .orElseThrow(() -> new RuntimeException("Section not found"));
        }
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
        employee.setKakaoUuid(user.getKakaoUuid()); // kakaoUuid 설정

        // 프로필 이미지가 있을 경우, byte[]로 변환하여 설정
        if (profileImage != null && !profileImage.isEmpty()) {
            employee.setProfileImage(profileImage.getBytes());
        }
        log.info("User {} kakaoUuid: {}", userId, user.getKakaoUuid());

        // 직원 저장 후 반환
        return employeeRepository.save(employee);
    }

    // 기존 직원의 kakaoUuid 업데이트 메서드
    @Transactional
    public boolean updateKakaoUuid(Long employeeId, String kakaoUuid) {
        // 직원 정보 조회
        EmployeeEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // userId를 기준으로 rawUserId를 찾는 방법
        String rawUserId = employee.getUser().getUserId().substring(6); // "kakao_" 부분을 제거한 ID

        // UserEntity에서 kakaoUuid 업데이트
        userRepository.updateKakaoUuid(kakaoUuid, rawUserId); // 유저의 kakaoUuid 업데이트

        // EmployeeEntity에 kakaoUuid도 업데이트
        employee.setKakaoUuid(kakaoUuid); // 직원의 kakaoUuid 업데이트
        employeeRepository.save(employee); // EmployeeEntity 저장

        // kakaoUuid 업데이트 로그
        log.info("Updating kakaoUuid for employeeId: {} to {}", employeeId, kakaoUuid);

// userId에서 "kakao_"를 제거한 값 추출 후 로그로 출력
        log.info("Raw userId for kakaoUuid update: {}", rawUserId);

// User 엔티티의 kakaoUuid 갱신 로그
        log.info("User kakaoUuid updated for userId: {}", rawUserId);
        return true;
    }

    // 직원 정보 조회 메소드
    public EmployeeEntity getEmployeeInfo(String userId) {
        EmployeeEntity employee = employeeRepository.findByUser_UserId(userId);
        // employee가 null일 경우 예외 처리

        if (employee == null) {
            // 사용자에게 적절한 예외나 메시지 반환
            throw new RuntimeException("Employee not found for userId: " + userId);
        }

        if (employee != null && employee.getProfileImage() != null) {
            // 프로필 이미지가 있을 경우 Base64로 변환하여 set
            String profileImageBase64 = Base64.getEncoder().encodeToString(employee.getProfileImage());
            employee.setProfileImageBase64(profileImageBase64);
        }

        // kakaoUuid 값이 null이 아닌지 확인
        String kakaoUuid = employee.getKakaoUuid();
        if (kakaoUuid != null) {
            System.out.println("Kakao UUID: " + kakaoUuid);  // 출력 확인
            log.info("Employee {} kakaoUuid: {}", userId, kakaoUuid);
        } else {
            System.out.println("Kakao UUID not found for userId: " + userId);
            log.info("Employee {} kakaoUuid not found", userId);
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

    @Transactional
    public boolean updateEmployee(Long employeeId, String name, String phone, String departmentName, String sectionName, String position, MultipartFile profileImage) throws IOException {
        Optional<EmployeeEntity> employeeOptional = employeeRepository.findById(employeeId);

        // 직원 정보가 존재하지 않으면 false 반환
        if (employeeOptional.isEmpty()) {
            System.out.println("Employee not found with ID: " + employeeId);
            return false;
        }
        EmployeeEntity employee = employeeOptional.get();

        // ✅ 🟢 새 부서 찾기 (부서 변경 여부 확인)
        DepartmentEntity newDepartmentEntity = employee.getDepartment();
        if (departmentName != null) {
            Optional<DepartmentEntity> departmentOptional = departmentRepository.findByDepartmentName(departmentName);
            if (departmentOptional.isPresent()) {
                newDepartmentEntity = departmentOptional.get();
                if (!newDepartmentEntity.equals(employee.getDepartment())) {
                    System.out.println("🔄 부서 변경 감지: " + employee.getDepartment().getDepartmentName() + " → " + newDepartmentEntity.getDepartmentName());
                    employee.setDepartment(newDepartmentEntity); // 🔥 부서 변경 적용
                }
            } else {
                System.out.println("❌ 해당 부서를 찾을 수 없음: " + departmentName);
                return false;
            }
        }

        // ✅ 🟢 새 섹션 찾기 (부서 변경 후 조회)
        // 이미 섹션이 null인 경우 섹션 업데이트를 건너뜁니다.
        if (employee.getSection() != null) {
            // 섹션이 null이거나 빈 문자열이면, employee의 섹션을 null로 설정
            if (sectionName == null || sectionName.trim().isEmpty()) {
                employee.setSection(null);
                System.out.println("ℹ 섹션 값이 비어있어 null로 설정합니다.");
            } else {
                Optional<SectionEntity> sectionOptional = sectionRepository.findBySectionNameAndDepartment(sectionName, newDepartmentEntity);
                if (sectionOptional.isPresent()) {
                    SectionEntity newSectionEntity = sectionOptional.get();
                    employee.setSection(newSectionEntity);
                    System.out.println("✔ 섹션 변경 완료: " + newSectionEntity.getSectionName());
                } else {
                    System.out.println("❌ 새로운 부서(" + newDepartmentEntity.getDepartmentName() + ")에서 섹션을 찾을 수 없음: " + sectionName);
                    return false;
                }
            }
        } else {
            // 섹션이 이미 null이면, 아무런 섹션 업데이트도 하지 않습니다.
            System.out.println("ℹ 직원의 섹션이 이미 null입니다. 부서만 업데이트합니다.");
        }

        // ✅ 🟢 직원 정보 업데이트
        if (name != null) employee.setName(name);
        if (phone != null) employee.setPhone(phone);
        if (position != null) employee.setPosition(position);

        // ✅ 🟢 프로필 이미지 처리
        if (profileImage != null && !profileImage.isEmpty()) {
            employee.setProfileImage(profileImage.getBytes());
        }

        employeeRepository.save(employee);
        employeeRepository.flush(); // 즉시 DB에 반영
        System.out.println("✅ 직원 정보 업데이트 완료: " + employeeId);
        return true;
    }

    @Transactional
    public boolean deleteEmployee(Long employeeId) {
        Optional<EmployeeEntity> employeeOptional = employeeRepository.findById(employeeId);
        if (employeeOptional.isPresent()) {
            employeeRepository.deleteById(employeeId);
            return true;
        }
        return false;
    }

    // 전체 직원의 userId 목록 반환
    public List<String> getAllEmployeeIds() {
        return getAllEmployees().stream()
                .map(employee -> employee.getUser().getUserId())
                .collect(Collectors.toList());
    }

    /**
     * 📌 전체 직원의 kakao_uuid 목록 반환
     */
    public List<String> getAllKakaoUuids() {
        List<String> uuids = employeeRepository.findAll().stream()
                .map(EmployeeEntity::getKakaoUuid)  // EmployeeEntity에서 바로 kakaoUuid를 가져옴
                .filter(kakaoUuid -> kakaoUuid != null && !kakaoUuid.isEmpty()) // null 값 필터링
                .collect(Collectors.toList());
        System.out.println("모든 직원 UUID 목록: " + uuids); // 로그 추가
        return uuids;
    }

    /**
     * 📌 특정 부서에 속한 직원들의 kakao_uuid 목록 반환
     */
    public List<String> getKakaoUuidsByDepartment(String departmentName) {
        // 로그 추가
        System.out.println("부서 이름으로 UUID 조회: " + departmentName);

        List<EmployeeEntity> employees = employeeRepository.findByDepartment_DepartmentName(departmentName);

        // 직원 목록 로깅
        System.out.println("조회된 직원 수: " + employees.size());
        employees.forEach(emp -> System.out.println("직원 정보: " + emp.getName() + ", UUID: " + (emp.getUser() != null ? emp.getUser().getKakaoUuid() : "null")));

        List<String> uuids = employees.stream()
                .filter(emp -> emp.getUser() != null)
                .map(emp -> emp.getUser().getKakaoUuid()) // user 엔티티에서 UUID 가져오기
                .filter(uuid -> uuid != null && !uuid.isEmpty())
                .collect(Collectors.toList());
// 부서 이름으로 UUID 조회 후 로그
        log.info("부서 이름으로 UUID 조회: {}", departmentName);

// 조회된 직원 수 및 부서의 UUID 목록 로그
        log.info("조회된 직원 수: {}", employees.size());
        log.info("부서 {}의 유효한 UUID 목록: {}", departmentName, uuids);
        System.out.println("부서 " + departmentName + "의 유효한 UUID 목록: " + uuids);
        return uuids;
    }

    // 특정 직원들의 카카오 UUID 목록을 가져오는 메서드
    public List<String> getKakaoUuidsWithEmployees(List<String> kakaoUuids) {
        // 1️⃣ 카카오 UUID로 직원 조회
        System.out.println("🔍 카카오 UUID로 조회하려는 값: " + kakaoUuids);

        List<EmployeeEntity> employees = employeeRepository.findByKakaoUuidIn(kakaoUuids);

        // 2️⃣ 직원 조회 후 로그
        employees.forEach(e -> System.out.println("✅ 직원 데이터: " + e.getName() + ", kakaoUuid: " + e.getKakaoUuid()));

        // 3️⃣ 유효한 kakaoUuid만 필터링해서 반환
        List<String> validKakaoUuids = employees.stream()
                .map(EmployeeEntity::getKakaoUuid)
                .filter(uuid -> uuid != null && !uuid.isEmpty())
                .collect(Collectors.toList());
// 입력된 kakaoUuids 값 로그
        log.info("카카오 UUID로 조회하려는 값: {}", kakaoUuids);


// 조회된 직원들의 kakaoUuid 값 로그
        log.info("조회된 직원들의 카카오 UUID 목록: {}", employees.stream()
                .map(EmployeeEntity::getKakaoUuid)  // Employee 객체에서 kakaoUuid를 추출
                .collect(Collectors.toList()));
        // 4️⃣ 유효한 kakaoUuid 목록 로그 출력
        System.out.println("🔍 최종 유효한 kakaoUuid 목록: " + validKakaoUuids);
        return validKakaoUuids;
    }


    public List<EmployeeRequestDTO> searchEmployees(String keyword, String department) {
        List<EmployeeEntity> employeeEntities;
        if (department != null && !department.isEmpty()) {
            // 부서 이름도 포함하여 검색하면서 직원의 전체 정보를 가져옵니다.
            employeeEntities = employeeRepository.findByNameContainingAndDepartment_DepartmentName(keyword, department);
        } else {
            // 이름만 기준으로 검색하면서 직원의 전체 정보를 가져옵니다.
            employeeEntities = employeeRepository.findByNameContaining(keyword);
        }

        // EmployeeEntity 리스트를 EmployeeDTO 리스트로 변환
        return employeeEntities.stream()
                .map(this::toDTO)  // toDTO() 메서드를 사용하여 변환
                .collect(Collectors.toList());
    }

    public EmployeeRequestDTO toDTO(EmployeeEntity employee) {
        EmployeeRequestDTO dto = new EmployeeRequestDTO();
        dto.setId(employee.getId());
        dto.setName(employee.getName());
        dto.setPhone(employee.getPhone());
        dto.setPosition(employee.getPosition());
        dto.setDepartmentName(employee.getDepartment() != null ? employee.getDepartment().getDepartmentName() : "부서 없음");
        dto.setSectionName(employee.getSection() != null ? employee.getSection().getSectionName() : "구역 없음");

        // ✅ 변환 확인 로그
        System.out.println("🔍 변환된 직원 DTO: " + employee.getName() + ", kakaoUuid: " + employee.getKakaoUuid());

        dto.setKakaoUuid(employee.getKakaoUuid());
        return dto;
    }


    @Transactional
    public EmployeeEntity getEmployeeByUserId(String userId) {
        return employeeRepository.findEmployeeWithUser(userId)
                .orElseThrow(() -> new RuntimeException("해당 직원이 존재하지 않습니다."));
    }


}
