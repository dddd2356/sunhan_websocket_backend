package kakao.login.dto.request.auth;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// 섹션 수정 요청 객체 (기존 섹션명과 새로운 섹션명)
public class SectionUpdateRequestDto {
    private String oldSectionName;
    private String newSectionName;
    private Long departmentId;       // 부서 ID
    private Long sectionId;          // 섹션 ID (수정하려는 섹션을 명확히 하기 위해 필요)
    // Getters and Setters
}
