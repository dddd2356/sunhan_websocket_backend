package kakao.login.entity;


import jakarta.persistence.*;
import kakao.login.dto.request.auth.SignUpRequestDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name="user")
@Table(name="user")
public class UserEntity {
    @Id
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId; // userId를 기본 키로 설정

    private String password;
    private String email;
    private String type;
    @Column(name = "role", nullable = false)
    private String role;

    public UserEntity(SignUpRequestDto dto){
        this.userId = dto.getId();
        this.password = dto.getPassword();
        this.email = dto.getEmail();
        this.type = "app"; //sns로그인 작업예정이어서 app으로
        this.role = "ROLE_USER";
    }

    public UserEntity(String userId, String email, String type){
        this.userId = userId;
        this.password = "Passw0rd";
        this.email = email;
        this.type = type; //sns로그인 작업예정이어서 app으로
        this.role = "ROLE_USER";
    }

}
