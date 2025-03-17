package kakao.login.dto.request.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
// 로그아웃 요청 객체
public class LogOutRequestDto {
    private String accessToken; // 카카오 또는 네이버 액세스토큰
}