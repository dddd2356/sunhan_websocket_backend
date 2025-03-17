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

    // DepartmentRepository와 SectionRepository를 주입받아 사용
    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    // 섹션 추가 메서드
    // 부서에 섹션을 추가하는 POST 방식으로 요청을 처리
    public DepartmentEntity addSection(Long departmentId, String sectionName) {
        // 부서 ID로 부서 조회
        Optional<DepartmentEntity> departmentOpt = departmentRepository.findById(departmentId);

        // 부서가 존재하지 않으면 null 반환
        if (!departmentOpt.isPresent()) {
            return null;
        }

        // 부서 정보 추출
        DepartmentEntity department = departmentOpt.get();

        // 새로운 섹션 객체 생성
        SectionEntity newSection = new SectionEntity();
        newSection.setSectionName(sectionName);  // 섹션 이름 설정
        newSection.setDepartment(department);    // 부서와 섹션 연결

        // 섹션 저장
        sectionRepository.save(newSection);

        // 부서의 flag 값을 "update"로 설정하여 수정 상태로 변경
        department.setFlag("update");
        departmentRepository.save(department);

        return department;  // 부서와 섹션 추가 후 수정된 부서 반환
    }

    // 섹션 삭제 메서드
    // 부서에서 섹션을 삭제
    public DepartmentEntity removeSection(Long departmentId, String sectionName) {
        // 부서 ID로 부서 조회
        Optional<DepartmentEntity> departmentOpt = departmentRepository.findById(departmentId);

        // 부서가 존재하지 않으면 null 반환
        if (!departmentOpt.isPresent()) {
            return null;
        }

        // 부서 정보 추출
        DepartmentEntity department = departmentOpt.get();

        // 섹션 이름으로 섹션 조회
        Optional<SectionEntity> sectionOpt = sectionRepository.findBySectionName(sectionName);

        // 섹션이 존재하지 않으면 null 반환
        if (!sectionOpt.isPresent()) {
            return null;
        }

        // 섹션 삭제
        SectionEntity section = sectionOpt.get();
        sectionRepository.delete(section);

        // 부서의 flag 값을 "update"로 설정하여 수정 상태로 변경
        department.setFlag("update");
        departmentRepository.save(department);

        return department;  // 부서와 섹션 삭제 후 수정된 부서 반환
    }

    // 섹션 수정 메서드
    // 기존 섹션 이름을 새로운 이름으로 수정
    public DepartmentEntity updateSection(Long departmentId, SectionUpdateRequestDto request) {
        // 부서 ID로 부서 조회
        DepartmentEntity department = departmentRepository.findById(departmentId).orElse(null);

        // 부서가 존재하지 않으면 null 반환
        if (department == null) {
            return null;
        }

        // 섹션 ID로 섹션 조회
        SectionEntity section = sectionRepository.findById(request.getSectionId()).orElse(null);

        // 섹션이 존재하지 않으면 null 반환
        if (section == null) {
            return null;
        }

        // 섹션 이름을 새로운 이름으로 수정
        section.setSectionName(request.getNewSectionName());

        // 부서 이름을 확인하여 부서와 섹션을 함께 갱신
        String departmentName = department.getDepartmentName(); // 부서 이름 가져오기

        // 섹션 수정 후 저장
        sectionRepository.save(section);  // 섹션 저장
        departmentRepository.save(department);  // 부서 저장

        return department;  // 수정된 부서 반환 (부서 이름 포함)
    }

    // 부서 ID로 부서 조회
    public DepartmentEntity getDepartmentById(Long departmentId) {
        // 부서 ID로 부서 찾기
        return departmentRepository.findById(departmentId).orElse(null);
    }
}
