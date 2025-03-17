package kakao.login.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/naver")
@CrossOrigin(origins = "http://localhost:3000") // 프론트엔드 도메인 허용
public class NaverController {

    // 네이버 사용자 정보 조회 API
    @GetMapping("/userinfo")
    public ResponseEntity<?> getNaverUserInfo(@RequestHeader("Authorization") String accessToken) {
        String url = "https://openapi.naver.com/v1/nid/me"; // 네이버 사용자 정보 API URL

        // 요청 헤더에 Authorization 설정 (accessToken)
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);

        // HttpEntity로 요청 헤더 설정
        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            // 네이버 API에 GET 요청을 보내고, 응답을 Map 형식으로 받음
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return ResponseEntity.ok(response.getBody()); // 성공 시 사용자 정보 반환
        } catch (Exception e) {
            // 예외 발생 시, 내부 서버 오류 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}