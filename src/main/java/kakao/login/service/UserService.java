package kakao.login.service;

import kakao.login.entity.UserEntity;
import kakao.login.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;  // UserRepository 주입

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 모든 유저 조회 메서드
    public List<UserEntity> findAllUsers() {
        return userRepository.findAll();  // UserRepository에서 모든 유저를 조회
    }
}
