package kakao.login.controller;

import kakao.login.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        String userId = authentication.getName(); // 예: kakao_3924653164, naver_[id]
        String employeeName = userService.getEmployeeName(userId); // employee.name, 없으면 null

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", authentication.isAuthenticated());
        response.put("authorities", authentication.getAuthorities());
        response.put("principal", userId);
        if (employeeName != null) {
            response.put("name", employeeName); // employee.name이 있을 때만 추가
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/role/{userId}")
    public ResponseEntity<Map<String, Object>> getUserRole(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();
        String role = userService.getUserRole(userId);
        response.put("role", role);
        return ResponseEntity.ok(response);
    }
}