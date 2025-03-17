package kakao.login.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// 인증번호 확인 요청 객체
public class CheckCertificationRequestDto {

    @NotBlank
    private String id; // 사용자 ID

    @Email
    @NotBlank
    private String email; // 사용자 이메일

    @NotBlank
    private String certificationNumber; // 인증번호
}