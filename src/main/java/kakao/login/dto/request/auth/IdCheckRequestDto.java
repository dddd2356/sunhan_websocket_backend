package kakao.login.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// ID 중복 확인 요청 객체
public class IdCheckRequestDto {
    @NotBlank
    private String id; // 사용자 ID
}