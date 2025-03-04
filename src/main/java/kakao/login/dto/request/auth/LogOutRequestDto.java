package kakao.login.dto.request.auth;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LogOutRequestDto {
    private String accessToken; //카카오 or 네이버 액세스토큰
}
