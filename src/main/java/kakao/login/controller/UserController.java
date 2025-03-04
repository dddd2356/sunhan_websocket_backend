package kakao.login.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    // 현재 인증된 사용자 정보를 반환하는 엔드포인트
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        // authentication.getName() 또는 인증 객체를 이용해 사용자 정보를 조회할 수 있습니다.
        // 예: 데이터베이스에서 사용자 엔티티를 조회 후 필요한 정보만 반환

        // 여기서는 간단하게 인증 객체의 정보를 그대로 반환하는 예시입니다.
        return ResponseEntity.ok(authentication);
    }
}
