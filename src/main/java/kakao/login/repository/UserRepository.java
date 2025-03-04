package kakao.login.repository;

import kakao.login.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    UserEntity findByUserId(String userId);

    List<UserEntity> findAllByRole(String role); // 특정 역할을 가진 모든 사용자 조회

    boolean existsByUserId(String userId);

    List<UserEntity> findAll();
}
