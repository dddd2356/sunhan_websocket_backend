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
        return departmentRepository.findAllDepartmentNames();
    }

    // 특정 부서에 해당하는 구역 목록 조회
    public List<String> getSectionsByDepartment(String department) {
        return sectionRepository.findSectionsByDepartment(department);
    }


}