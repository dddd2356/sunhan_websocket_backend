package kakao.login.controller;

import kakao.login.entity.EmployeeEntity;
import kakao.login.repository.EmployeeRepository;
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
    private final EmployeeRepository employeeRepository; // 추가

    @Autowired
    public UserController(UserService userService, EmployeeRepository employeeRepository) {
        this.userService = userService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        String userId = authentication.getName();
        EmployeeEntity employee = employeeRepository.findByUser_UserId(userId);
        if (employee == null) {
            throw new RuntimeException("Employee not found for userId: " + userId);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", authentication.isAuthenticated());
        response.put("authorities", authentication.getAuthorities());
        response.put("principal", userId);
        response.put("id", employee.getId());
        response.put("name", employee.getName());
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