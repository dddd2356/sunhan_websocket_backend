package kakao.login.controller;

import kakao.login.entity.DepartmentEntity;
import kakao.login.entity.EmployeeEntity;
import kakao.login.repository.DepartmentRepository;
import kakao.login.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/detail/department")
public class DepartmentController {

    @Autowired
    private DepartmentRepository departmentRepository;


    // 부서 목록 조회 (flag가 "delete"인 항목은 제외)
    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentEntity>> getDepartments() {
        List<DepartmentEntity> departments = departmentRepository.findByFlagNot("delete");
        return ResponseEntity.ok(departments);
    }

    // 부서 추가 (신규 부서는 flag를 "add"로 설정)
    @PostMapping("/add")
    public ResponseEntity<DepartmentEntity> addDepartment(@RequestBody DepartmentEntity department) {
        department.setFlag("add");
        DepartmentEntity savedDepartment = departmentRepository.save(department);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDepartment);
    }

    // 부서 수정 (수정 시 flag를 "update"로 변경)
    @PutMapping("/{departmentId}/update")
    public ResponseEntity<DepartmentEntity> updateDepartment(
            @PathVariable Long departmentId,
            @RequestBody DepartmentEntity departmentDetails) {

        Optional<DepartmentEntity> departmentOpt = departmentRepository.findById(departmentId);
        if (!departmentOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        DepartmentEntity department = departmentOpt.get();
        // 필요에 따라 부서명을 수정할 수 있지만, 기본 키가 부서명이므로 주의가 필요합니다.
        department.setDepartmentName(departmentDetails.getDepartmentName());
        department.setFlag("update");
        DepartmentEntity updatedDepartment = departmentRepository.save(department);
        return ResponseEntity.ok(updatedDepartment);
    }

    // 부서 삭제 (실제 삭제하지 않고 flag를 "delete"로 업데이트)
    @PutMapping("/{departmentId}/delete")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long departmentId) {
        Optional<DepartmentEntity> departmentOpt = departmentRepository.findById(departmentId);
        if (!departmentOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        DepartmentEntity department = departmentOpt.get();
        department.setFlag("delete");
        departmentRepository.save(department);
        return ResponseEntity.ok().build();
    }
}