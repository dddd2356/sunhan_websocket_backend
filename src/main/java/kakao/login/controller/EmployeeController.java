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
            @RequestParam(required = false) String section,  // 섹션 파라미터를 선택사항으로 변경
            @RequestParam String position,
            @RequestParam(required = false) MultipartFile profileImage) {

        try {
            EmployeeEntity newEmployee = employeeService.registerEmployee(userId, name, phone, department, section, position, profileImage);
            return ResponseEntity.ok("직원 등록이 완료되었습니다.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 업로드 오류");
        }
    }

    // 직원 정보 조회 API
    @GetMapping("/{userId}")
    public ResponseEntity<EmployeeEntity> getEmployeeInfo(@PathVariable String userId) {
        try {
            EmployeeEntity employee = employeeService.getEmployeeInfo(userId);
            if (employee == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok(employee);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<EmployeeEntity>> getAllEmployees() {
        try {
            List<EmployeeEntity> employees = employeeService.getAllEmployees();
            return ResponseEntity.ok(employees);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 부서에 해당하는 모든 직원 목록 조회
    @GetMapping("/department/employees")
    public ResponseEntity<List<EmployeeRequestDTO>> getEmployeesByDepartment(
            @RequestParam String department) {
        // Use your existing service method
        List<EmployeeEntity> employees = employeeService.getEmployeesByDepartment(department);
        List<EmployeeRequestDTO> employeeDTOs = employees.stream()
                .map(EmployeeRequestDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(employeeDTOs);
    }

    // 부서에 해당하는 구역 목록 조회
    @GetMapping("/department/sections")
    public List<String> getSectionsByDepartment(@RequestParam String department) {
        return departmentService.getSectionsByDepartment(department);
    }


    // 구역에 해당하는 직원 목록 조회
    @GetMapping("/department/section/employees")
    public ResponseEntity<List<EmployeeRequestDTO>> getEmployeesBySection(
            @RequestParam String department,
            @RequestParam String section) {
        System.out.println("Received department: " + department);
        System.out.println("Received section: " + section);

        List<EmployeeEntity> employees = employeeService.getEmployeesByDepartmentAndSection(department, section);
        List<EmployeeRequestDTO> employeeDTOs = employees.stream()
                .map(EmployeeRequestDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(employeeDTOs);
    }





    //조직도 수정에서 카드 직원 클릭하면 수정, 삭제 하는 기능
    // ✏ 직원 수정 API
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/employee/{employeeId}/update")
    public ResponseEntity<String> updateEmployee(
            @PathVariable Long employeeId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String section, // 선택 사항으로 변경
            @RequestParam(required = false) String position,
            @RequestParam(required = false) MultipartFile profileImage) {

        try {
            boolean updated = employeeService.updateEmployee(employeeId, name, phone, department, section, position, profileImage);
            if (updated) {
                return ResponseEntity.ok("직원 정보가 수정되었습니다.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("직원을 찾을 수 없습니다.");
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 업로드 오류");
        }
    }



    @DeleteMapping("/employee/{employeeId}/delete")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long employeeId) {
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
        List<EmployeeRequestDTO> employeeDTOs = employeeService.searchEmployees(keyword, department);
        return ResponseEntity.ok(employeeDTOs);
    }

    @GetMapping("/kakao/{userId}")
    public EmployeeEntity getEmployee(@PathVariable String userId) {
        return employeeService.getEmployeeByUserId(userId);
    }

    // 카카오 UUID 업데이트 API
    @PutMapping("/employee/{employeeId}/updateKakaoUuid")
    public ResponseEntity<String> updateKakaoUuid(
            @PathVariable Long employeeId,
            @RequestBody EmployeeRequestDTO requestDTO) {
        try {
            boolean updated = employeeService.updateKakaoUuid(employeeId, requestDTO.getKakaoUuid());
            if (updated) {
                return ResponseEntity.ok("카카오 UUID가 성공적으로 업데이트되었습니다.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("직원을 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("카카오 UUID 업데이트 오류");
        }
    }

}