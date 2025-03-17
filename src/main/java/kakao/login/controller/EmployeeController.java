package kakao.login.controller;

import jakarta.transaction.Transactional;
import kakao.login.dto.request.message.EmployeeRequestDTO;
import kakao.login.entity.EmployeeEntity;
import kakao.login.service.DepartmentService;
import kakao.login.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/detail/employment")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private DepartmentService departmentService;

    // 직원 등록 API
    @PostMapping("/sign-up")
    @Transactional
    public ResponseEntity<String> registerEmployee(
            @RequestParam String userId,
            @RequestParam String name,
            @RequestParam String phone,
            @RequestParam String department,
            @RequestParam String section,
            @RequestParam String position,
            @RequestParam(required = false) MultipartFile profileImage) {

        try {
            // 직원 등록 서비스 호출
            EmployeeEntity newEmployee = employeeService.registerEmployee(userId, name, phone, department, section, position, profileImage);
            return ResponseEntity.ok("직원 등록이 완료되었습니다.");
        } catch (IOException e) {
            // 파일 업로드 오류 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 업로드 오류");
        }
    }

    // 직원 정보 조회 API
    @GetMapping("/{userId}")
    public ResponseEntity<EmployeeEntity> getEmployeeInfo(@PathVariable String userId) {
        try {
            // 직원 정보를 조회하고, 없으면 404 반환
            EmployeeEntity employee = employeeService.getEmployeeInfo(userId);
            if (employee == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok(employee);
        } catch (Exception e) {
            // 예외 발생 시 내부 서버 오류 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 모든 직원 조회 API
    @GetMapping("/all")
    public ResponseEntity<List<EmployeeEntity>> getAllEmployees() {
        try {
            // 모든 직원 조회
            List<EmployeeEntity> employees = employeeService.getAllEmployees();
            return ResponseEntity.ok(employees);
        } catch (Exception e) {
            // 예외 발생 시 내부 서버 오류 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 부서에 해당하는 구역 목록 조회
    @GetMapping("/department/sections")
    public List<String> getSectionsByDepartment(@RequestParam String department) {
        // 부서에 속한 구역 목록을 조회
        return departmentService.getSectionsByDepartment(department);
    }

    // 구역에 해당하는 직원 목록 조회
    @GetMapping("/department/section/employees")
    public ResponseEntity<List<EmployeeRequestDTO>> getEmployeesBySection(
            @RequestParam String department,
            @RequestParam String section) {
        // 구역별 직원 목록 조회 후 DTO로 변환하여 반환
        List<EmployeeEntity> employees = employeeService.getEmployeesByDepartmentAndSection(department, section);
        List<EmployeeRequestDTO> employeeDTOs = employees.stream()
                .map(EmployeeRequestDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(employeeDTOs);
    }

    // 직원 수정 API (관리자만 접근 가능)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/employee/{employeeId}/update")
    public ResponseEntity<String> updateEmployee(
            @PathVariable Long employeeId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) MultipartFile profileImage) {

        try {
            // 직원 수정 서비스 호출
            boolean updated = employeeService.updateEmployee(employeeId, name, phone, department, section, position, profileImage);
            if (updated) {
                return ResponseEntity.ok("직원 정보가 수정되었습니다.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("직원을 찾을 수 없습니다.");
            }
        } catch (IOException e) {
            // 파일 업로드 오류 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 업로드 오류");
        }
    }

    // 직원 삭제 API
    @DeleteMapping("/employee/{employeeId}/delete")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long employeeId) {
        // 직원 삭제 처리
        boolean deleted = employeeService.deleteEmployee(employeeId);
        if (deleted) {
            return ResponseEntity.ok("직원이 삭제되었습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("직원을 찾을 수 없습니다.");
        }
    }

    // 직원 검색 API
    @GetMapping("/search")
    public ResponseEntity<List<EmployeeRequestDTO>> searchEmployees(
            @RequestParam String keyword,
            @RequestParam(required = false) String department) {
        // 검색어로 직원 조회
        List<EmployeeRequestDTO> employeeDTOs = employeeService.searchEmployees(keyword, department);
        return ResponseEntity.ok(employeeDTOs);
    }

    @GetMapping("/kakao/{userId}")
    public EmployeeEntity getEmployee(@PathVariable String userId) {
        // 카카오 로그인 시 직원 정보 조회
        return employeeService.getEmployeeByUserId(userId);
    }

    // 카카오 UUID 업데이트 API
    @PutMapping("/employee/{employeeId}/updateKakaoUuid")
    public ResponseEntity<String> updateKakaoUuid(
            @PathVariable Long employeeId,
            @RequestBody EmployeeRequestDTO requestDTO) {
        try {
            // 카카오 UUID 업데이트 서비스 호출
            boolean updated = employeeService.updateKakaoUuid(employeeId, requestDTO.getKakaoUuid());
            if (updated) {
                return ResponseEntity.ok("카카오 UUID가 성공적으로 업데이트되었습니다.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("직원을 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            // 예외 발생 시 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("카카오 UUID 업데이트 오류");
        }
    }
}
