package kakao.login.controller;

import jakarta.servlet.http.HttpServletRequest;
import kakao.login.entity.UserEntity;
import kakao.login.provider.JwtProvider;
import kakao.login.repository.UserRepository;
import kakao.login.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController  // Controller 클래스에 @RestController 추가
//@RequiredArgsConstructor  // 생성자 주입을 위한 어노테이션 추가
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final UserService userService;  // UserService 주입
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;  // JwtProvider 변수 선언

    @Autowired
    public AdminController(UserService userService, UserRepository userRepository, JwtProvider jwtProvider) {
        this.userService = userService;
        this.userRepository = userRepository;  // 생성자 주입
        this.jwtProvider = jwtProvider;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserEntity>> getAllUsers() {
        List<UserEntity> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }
}