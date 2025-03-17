package kakao.login.service;

import kakao.login.entity.UserEntity;
import kakao.login.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;  // UserRepository 주입

    // 생성자를 통한 의존성 주입
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 모든 유저 조회 메서드
    public List<UserEntity> findAllUsers() {
        // UserRepository에서 모든 유저 정보를 조회하여 반환
        return userRepository.findAll();
    }

    // 특정 userId의 role 조회 메서드
    public String getUserRole(String userId) {
        // userId로 사용자 정보 조회
        UserEntity user = userRepository.findByUserId(userId);

        // 사용자가 존재하면 그 사용자의 역할(role)을 반환, 없으면 기본 "ROLE_USER" 반환
        if (user != null) {
            return user.getRole();
        } else {
            return "ROLE_USER";  // 기본 값
        }
    }
}
