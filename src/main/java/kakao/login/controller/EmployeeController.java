package kakao.login.controller;

import kakao.login.entity.EmployeeEntity;
import kakao.login.service.DepartmentService;
import kakao.login.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/detail/employment")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private DepartmentService departmentService;

    // 직원 등록 API
    @PostMapping("/sign-up")
    public ResponseEntity<String> registerEmployee(
            @RequestParam String userId,
            @RequestParam String name,
            @RequestParam String phone,
            @RequestParam String department,
            @RequestParam String section,
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

    // 부서에 해당하는 구역 목록 조회
    @GetMapping("/department/sections")
    public List<String> getSectionsByDepartment(@RequestParam String department) {
        return departmentService.getSectionsByDepartment(department);
    }


    // 구역에 해당하는 직원 목록 조회
    @GetMapping("/department/section/employees")
    public List<EmployeeEntity> getEmployeesBySection(
            @RequestParam String department,
            @RequestParam String section) {
        return employeeService.getEmployeesByDepartmentAndSection(department, section);
    }
}
