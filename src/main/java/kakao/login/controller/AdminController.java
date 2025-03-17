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

@RestController  // RESTful API 컨트롤러
@RequestMapping("/api/v1/admin")  // URL 경로 설정
public class AdminController {

    private final UserService userService;  // 사용자 서비스 주입
    private final UserRepository userRepository;  // 사용자 레포지토리 주입
    private final JwtProvider jwtProvider;  // JWT 프로바이더 주입

    @Autowired  // 생성자 주입
    public AdminController(UserService userService, UserRepository userRepository, JwtProvider jwtProvider) {
        this.userService = userService;
        this.userRepository = userRepository;  // 생성자 주입
        this.jwtProvider = jwtProvider;
    }

    // 모든 사용자 목록을 조회하는 엔드포인트
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")  // ADMIN 역할만 접근 가능
    public ResponseEntity<List<UserEntity>> getAllUsers() {
        List<UserEntity> users = userService.findAllUsers();  // 사용자 목록 조회
        return ResponseEntity.ok(users);  // 성공적으로 조회한 사용자 목록 반환
    }
}
