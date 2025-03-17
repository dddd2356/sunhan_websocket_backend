package kakao.login.service;

import kakao.login.repository.DepartmentRepository;
import kakao.login.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    // 모든 부서 이름을 조회하는 메서드
    public List<String> getAllDepartments() {
        // departmentRepository에서 모든 부서 이름을 조회하여 반환
        return departmentRepository.findAllDepartmentNames();
    }

    // 특정 부서에 해당하는 구역 목록을 조회하는 메서드
    public List<String> getSectionsByDepartment(String department) {
        // sectionRepository에서 해당 부서에 속한 섹션 이름을 조회하여 반환
        return sectionRepository.findSectionsByDepartment(department);
    }
}