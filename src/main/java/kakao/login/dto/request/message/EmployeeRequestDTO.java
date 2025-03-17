package kakao.login.dto.request.message;

import kakao.login.entity.EmployeeEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Base64;

@Getter
@Setter
@NoArgsConstructor
// 직원 정보를 전달하는 DTO
public class EmployeeRequestDTO {
    private Long id;                // 직원 ID
    private String name;            // 직원 이름
    private String phone;           // 직원 전화번호
    private String position;        // 직원 직급
    private String departmentName;  // 직원 소속 부서 이름
    private String sectionName;     // 직원 소속 섹션 이름
    private String profileImage;    // 직원 프로필 이미지 (Base64 인코딩된 이미지)
    private String kakaoUuid;       // 직원 카카오 UUID

    // 엔티티(EmployeeEntity) 객체를 DTO로 변환하는 헬퍼 메서드
    public static EmployeeRequestDTO fromEntity(EmployeeEntity employee) {
        EmployeeRequestDTO dto = new EmployeeRequestDTO();
        dto.setId(employee.getId());
        dto.setName(employee.getName());
        dto.setPosition(employee.getPosition());
        dto.setPhone(employee.getPhone());
        dto.setDepartmentName(employee.getDepartment().getDepartmentName());
        dto.setSectionName(employee.getSection() != null ? employee.getSection().getSectionName() : "구역 없음");

        // 프로필 이미지가 있을 경우 Base64로 인코딩하여 설정
        if (employee.getProfileImage() != null && employee.getProfileImage().length > 0) {
            String base64Image = Base64.getEncoder().encodeToString(employee.getProfileImage());
            dto.setProfileImage(base64Image);
        } else {
            dto.setProfileImage(null); // 프로필 이미지가 없으면 null 설정
        }

        dto.setKakaoUuid(employee.getKakaoUuid()); // 카카오 UUID 설정
        return dto;
    }
}