package kakao.login.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// 이메일 인증 요청 객체
public class EmailCertificationRequestDto {

    @NotBlank
    private String id; // 사용자 ID

    @Email
    @NotBlank
    private String email; // 사용자 이메일
}