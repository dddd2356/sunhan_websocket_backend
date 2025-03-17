package kakao.login.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
// 카카오 API 응답 DTO
public class KakaoApiResponse {
    // 성공 여부를 나타내는 필드
    private boolean success;
    // 필요한 응답 필드 추가 가능 (예: 메시지, 데이터 등)
}
