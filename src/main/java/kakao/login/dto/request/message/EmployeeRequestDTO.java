package kakao.login.dto.request.message;

import kakao.login.entity.EmployeeEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Base64;

@Getter
@Setter
@NoArgsConstructor
public class EmployeeRequestDTO {
    private Long id;
    private String name;
    private String phone;
    private String position;
    private String departmentName;
    private String sectionName;
    private String profileImage;
    private String kakaoUuid;
    // 생성자, getter, setter 추가

    // 엔티티로부터 DTO를 생성하는 헬퍼 메서드
    public static EmployeeRequestDTO fromEntity(EmployeeEntity employee) {
        EmployeeRequestDTO dto = new EmployeeRequestDTO();
        dto.setId(employee.getId());
        dto.setName(employee.getName());
        dto.setPosition(employee.getPosition());
        dto.setPhone(employee.getPhone());
        dto.setDepartmentName(employee.getDepartment().getDepartmentName());
        dto.setSectionName(employee.getSection() != null ? employee.getSection().getSectionName() : "구역 없음");
        if (employee.getProfileImage() != null && employee.getProfileImage().length > 0) {
            String base64Image = Base64.getEncoder().encodeToString(employee.getProfileImage());
            dto.setProfileImage(base64Image);
        } else {
            dto.setProfileImage(null); // 혹은 기본 이미지 URL 등
        }
        dto.setKakaoUuid(employee.getKakaoUuid()); // 최신 카카오 UUID 설정
        return dto;
    }
}