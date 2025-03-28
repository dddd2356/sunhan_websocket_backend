package kakao.login.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kakao.login.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/naver")
@Tag(name = "Naver User Info API", description = "네이버 사용자 정보 확인하는 기능")
@CrossOrigin(origins = "http://localhost:3000")
public class NaverController {

    private final UserService userService;

    @Autowired
    public NaverController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/userinfo")
    @Operation(summary = "네이버로 로그인한 사용자 조회", description = "네이버로 로그인한 사용자 정보를 확인하는 API")
    public ResponseEntity<?> getNaverUserInfo(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid Authorization header"));
        }

        String accessToken = authHeader.replace("Bearer ", "");
        String url = "https://openapi.naver.com/v1/nid/me";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            Map<String, String> responseData = (Map<String, String>) body.get("response");

            String userId = "naver_" + responseData.get("id");
            String employeeName = userService.getEmployeeName(userId); // employee.name 조회
            String nickname = employeeName != null ? employeeName : responseData.get("name") != null ? responseData.get("name") : "Unknown";

            Map<String, String> result = new HashMap<>();
            result.put("userId", userId);
            result.put("nickname", nickname); // employee.name 없으면 네이버 name 사용
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch Naver user info: " + e.getMessage()));
        }
    }
}