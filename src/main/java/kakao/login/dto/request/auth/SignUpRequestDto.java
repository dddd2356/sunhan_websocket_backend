package kakao.login.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
// 회원가입 요청 객체
public class SignUpRequestDto {

    @NotBlank
    private String id; // 사용자 ID

    @NotBlank
    @Pattern(regexp = "^(?=.*[a-zA-Z=])(?=.*[0-9])[a-zA-z0-9]{8,13}$") // 영문자, 숫자 포함 8자리부터 13자리까지
    private String password; // 사용자 비밀번호

    @Email
    @NotBlank
    private String email; // 사용자 이메일

    @NotBlank
    private String certificationNumber; // 인증번호

    // 추가: 카카오 UUID 필드 (SNS 로그인 후 회원가입 시 같이 받고 싶은 경우)
    private String kakaoUuid;
}