package kakao.login.controller;

import kakao.login.dto.request.auth.SectionUpdateRequestDto;
import kakao.login.entity.DepartmentEntity;
import kakao.login.service.SectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/detail/department")
public class SectionController {

    @Autowired
    private SectionService sectionService;

    // 부서 상세 정보 조회 API 추가
    @GetMapping("/{departmentId}")
    public ResponseEntity<DepartmentEntity> getDepartmentDetail(@PathVariable Long departmentId) {
        // 부서 ID로 부서 정보 조회
        DepartmentEntity department = sectionService.getDepartmentById(departmentId);

        if (department == null) {
            // 부서가 없으면 404 반환
            return ResponseEntity.notFound().build();
        }
        // 부서가 존재하면 부서 정보 반환
        return ResponseEntity.ok(department);
    }

    // 섹션 추가 (POST 요청)
    @PostMapping("/{departmentId}/sections/add")
    public ResponseEntity<DepartmentEntity> addSection(
            @PathVariable Long departmentId,
            @RequestBody String sectionName) {

        // 섹션을 부서에 추가
        DepartmentEntity department = sectionService.addSection(departmentId, sectionName);
        if (department == null) {
            // 부서가 없거나 섹션 추가 실패 시 404 반환
            return ResponseEntity.notFound().build();
        }
        // 섹션 추가 후 부서 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(department);
    }

    // 섹션 삭제
    @PutMapping("/{departmentId}/sections/delete")
    public ResponseEntity<DepartmentEntity> removeSection(
            @PathVariable Long departmentId,
            @RequestBody String sectionName) {

        // 섹션을 부서에서 삭제
        DepartmentEntity department = sectionService.removeSection(departmentId, sectionName);
        if (department == null) {
            // 섹션이 없으면 400 오류 반환
            return ResponseEntity.badRequest().build();
        }
        // 섹션 삭제 후 부서 반환
        return ResponseEntity.ok(department);
    }

    // 섹션 수정
    @PutMapping("/{departmentId}/sections/update")
    public ResponseEntity<DepartmentEntity> updateSection(
            @PathVariable Long departmentId,
            @RequestBody SectionUpdateRequestDto request) {

        // 섹션 정보를 수정
        DepartmentEntity department = sectionService.updateSection(departmentId, request);
        if (department == null) {
            // 섹션 수정 실패 시 400 오류 반환
            return ResponseEntity.badRequest().build();
        }
        // 섹션 수정 후 부서 반환
        return ResponseEntity.ok(department);
    }
}
