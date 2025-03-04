package kakao.login.controller;

import kakao.login.entity.UserEntity;
import kakao.login.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController  // Controller 클래스에 @RestController 추가
@RequiredArgsConstructor  // 생성자 주입을 위한 어노테이션 추가
public class AdminController {
    private final UserService userService;  // UserService 주입

    @GetMapping("/api/v1/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserEntity>> getAllUsers() {
        List<UserEntity> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }
}
