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
        // "delete" flag가 아닌 부서 목록을 조회하여 반환
        List<DepartmentEntity> departments = departmentRepository.findByFlagNot("delete");
        return ResponseEntity.ok(departments);
    }

    // 부서 추가 (신규 부서는 flag를 "add"로 설정)
    @PostMapping("/add")
    public ResponseEntity<DepartmentEntity> addDepartment(@RequestBody DepartmentEntity department) {
        // 새 부서 생성 시 flag를 "add"로 설정
        department.setFlag("add");
        DepartmentEntity savedDepartment = departmentRepository.save(department);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDepartment);
    }

    // 부서 수정 (수정 시 flag를 "update"로 변경)
    @PutMapping("/{departmentId}/update")
    public ResponseEntity<DepartmentEntity> updateDepartment(
            @PathVariable Long departmentId,
            @RequestBody DepartmentEntity departmentDetails) {

        // 부서 ID로 해당 부서를 조회
        Optional<DepartmentEntity> departmentOpt = departmentRepository.findById(departmentId);
        if (!departmentOpt.isPresent()) {
            // 부서가 없으면 404 반환
            return ResponseEntity.notFound().build();
        }
        DepartmentEntity department = departmentOpt.get();
        // 부서명 수정 (주요 키가 부서명이므로 수정 시 주의 필요)
        department.setDepartmentName(departmentDetails.getDepartmentName());
        // flag를 "update"로 설정
        department.setFlag("update");
        DepartmentEntity updatedDepartment = departmentRepository.save(department);
        return ResponseEntity.ok(updatedDepartment);
    }

    // 부서 삭제 (실제 삭제하지 않고 flag를 "delete"로 업데이트)
    @PutMapping("/{departmentId}/delete")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long departmentId) {
        // 부서 ID로 해당 부서를 조회
        Optional<DepartmentEntity> departmentOpt = departmentRepository.findById(departmentId);
        if (!departmentOpt.isPresent()) {
            // 부서가 없으면 404 반환
            return ResponseEntity.notFound().build();
        }
        DepartmentEntity department = departmentOpt.get();
        // 부서를 삭제된 상태로 변경하기 위해 flag를 "delete"로 설정
        department.setFlag("delete");
        departmentRepository.save(department);
        return ResponseEntity.ok().build();
    }
}