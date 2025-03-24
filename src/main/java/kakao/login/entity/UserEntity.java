package kakao.login.entity;

import jakarta.persistence.*;
import kakao.login.dto.request.auth.SignUpRequestDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name="user")  // "user" 테이블과 매핑되는 엔티티 클래스
@Table(name="user")  // 테이블 이름 지정
public class UserEntity {

    @Id
    @Column(name = "user_id", nullable = false, unique = true)  // "user_id" 컬럼을 기본 키로 설정
    private String userId;  // 사용자 ID
    private String password;  // 비밀번호
    private String email;  // 이메일
    private String type;  // 사용자 유형 (예: "app", "sns" 등)

    @Column(name = "role", nullable = false)  // 역할을 나타내는 컬럼 (예: "ROLE_USER", "ROLE_ADMIN")
    private String role;

    @Column(name = "kakao_uuid", unique = true, nullable = true)  // 카카오 로그인 시 사용하는 UUID
    private String kakaoUuid;

    // SignUpRequestDto를 기반으로 UserEntity 객체를 생성하는 생성자
    public UserEntity(SignUpRequestDto dto){
        this.userId = dto.getId();
        this.password = dto.getPassword();
        this.email = dto.getEmail();
        this.type = "app";  // SNS 로그인 예정이므로 기본값을 "app"으로 설정
        this.role = "ROLE_USER";  // 기본 역할을 "ROLE_USER"로 설정
        this.kakaoUuid = dto.getKakaoUuid();        // SignUpRequestDto에서 받은 카카오 UUID 설정 (추가)
    }
    // Update constructor to remove refreshToken parameter
    public UserEntity(SignUpRequestDto dto, String refreshToken) {
        this(dto);
    }
}
