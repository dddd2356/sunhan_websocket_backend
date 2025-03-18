package kakao.login.service;

import kakao.login.dto.request.auth.SectionUpdateRequestDto;
import kakao.login.entity.DepartmentEntity;
import kakao.login.entity.SectionEntity;
import kakao.login.repository.DepartmentRepository;
import kakao.login.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SectionService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    // 섹션 추가
    // 섹션 추가 (POST로 변경)
    public DepartmentEntity addSection(Long departmentId, String sectionName) {
        Optional<DepartmentEntity> departmentOpt = departmentRepository.findById(departmentId);
        if (!departmentOpt.isPresent()) {
            return null;
        }

        DepartmentEntity department = departmentOpt.get();

        // 새로운 섹션을 생성
        SectionEntity newSection = new SectionEntity();
        newSection.setSectionName(sectionName);
        newSection.setDepartment(department); // 부서와 연결

        // 섹션 저장
        sectionRepository.save(newSection);

        // 부서의 flag를 "update"로 설정하여 수정 상태로 변경
        department.setFlag("update");
        departmentRepository.save(department);

        return department; // 부서와 섹션 추가 후 부서 반환
    }


    // 섹션 삭제
    public DepartmentEntity removeSection(Long departmentId, String sectionName) {
        Optional<DepartmentEntity> departmentOpt = departmentRepository.findById(departmentId);
        if (!departmentOpt.isPresent()) {
            return null;
        }

        DepartmentEntity department = departmentOpt.get();

        // 해당 부서에서 섹션을 찾기
        Optional<SectionEntity> sectionOpt = sectionRepository.findBySectionName(sectionName);
        if (!sectionOpt.isPresent()) {
            return null;  // 섹션이 없으면 null 반환
        }

        SectionEntity section = sectionOpt.get();

        // 섹션 삭제
        sectionRepository.delete(section);

        // 부서의 flag를 "update"로 설정하여 수정 상태로 변경
        department.setFlag("update");
        departmentRepository.save(department);

        return department;  // 부서와 섹션 삭제 후 부서 반환
    }

    // 섹션 수정
    public DepartmentEntity updateSection(Long departmentId, SectionUpdateRequestDto request) {
        DepartmentEntity department = departmentRepository.findById(departmentId).orElse(null);
        if (department == null) {
            return null; // 부서가 존재하지 않으면 null 반환
        }

        SectionEntity section = sectionRepository.findById(request.getSectionId()).orElse(null);
        if (section == null) {
            return null; // 섹션이 존재하지 않으면 null 반환
        }

        // 섹션 이름 수정
        section.setSectionName(request.getNewSectionName());

        // 부서 이름을 함께 포함시키기 위해 부서 정보를 다시 확인
        String departmentName = department.getDepartmentName(); // 부서 이름 가져오기

        // 섹션 수정 후, 부서 이름도 함께 갱신
        sectionRepository.save(section); // 섹션 저장
        departmentRepository.save(department); // 부서 저장

        return department; // 수정된 부서 반환 (부서 이름 포함)
    }


    public DepartmentEntity getDepartmentById(Long departmentId) {
        return departmentRepository.findById(departmentId).orElse(null);
    }
}
