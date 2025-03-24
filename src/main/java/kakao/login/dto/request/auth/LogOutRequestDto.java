package kakao.login.dto.request.auth;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LogOutRequestDto {
    private String userId;
    private String refreshToken;
}