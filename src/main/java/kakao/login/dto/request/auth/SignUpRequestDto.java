package kakao.login.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Setter
@Getter
@NoArgsConstructor
public class SignUpRequestDto {

    @NotBlank
    private String id;

    @NotBlank
    @Pattern(regexp = "^(?=.*[a-zA-Z=])(?=.*[0-9])[a-zA-z0-9]{8,13}$") //영문자, 숫자 포함 8자리부터 13자리까지
    private String password;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String certificationNumber;
}
