package kakao.login.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import kakao.login.entity.DepartmentEntity;
import kakao.login.entity.SectionEntity;
import kakao.login.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.Base64;

@Entity
@Table(name = "employee")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 직원 고유 ID

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    private String name;
    private String phone;

    @ManyToOne
    @JoinColumn(name = "department_id", referencedColumnName = "id")
    @JsonIgnore
    private DepartmentEntity department;

    @ManyToOne
    @JoinColumn(name = "section_id", referencedColumnName = "id")
    private SectionEntity section;

    private String position;

    @Lob
    @JsonIgnore
    @Column(name = "profile_image")
    private byte[] profileImage;

    // 카카오 UUID는 UserEntity에서 가져옵니다.
    @Column(name = "kakao_uuid")
    private String kakaoUuid;

    @Transient
    private String profileImageBase64;

    @JsonProperty("profile_image")
    public String getProfileImageBase64() {
        if (profileImage != null && profileImage.length > 0) {
            String base64 = Base64.getEncoder().encodeToString(profileImage);
            System.out.println("Base64 Encoded Image: " + base64.substring(0, 50) + "...");
            return base64;
        }
        System.out.println("profileImage is null or empty");
        return null;
    }

    public String getKakaoUuid() {
        // kakaoUuid는 String이어야 하므로 그대로 반환
        return user != null ? user.getKakaoUuid() : null;
    }
}
