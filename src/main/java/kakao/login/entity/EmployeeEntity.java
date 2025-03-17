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

import java.util.Base64;

@Entity
@Table(name = "employee")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// 직원 정보를 처리하는 엔티티
public class EmployeeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 직원 고유 ID

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;  // 사용자 정보 (UserEntity와 연관)

    private String name;  // 직원 이름
    private String phone;  // 직원 전화번호

    @ManyToOne
    @JoinColumn(name = "department_id", referencedColumnName = "id")
    @JsonIgnore
    private DepartmentEntity department;  // 부서 정보 (DepartmentEntity와 연관)

    @ManyToOne
    @JoinColumn(name = "section_id", referencedColumnName = "id")
    private SectionEntity section;  // 섹션 정보 (SectionEntity와 연관)

    private String position;  // 직급

    @Lob
    @JsonIgnore
    @Column(name = "profile_image")
    private byte[] profileImage;  // 프로필 이미지 (이미지 데이터를 바이트 배열로 저장)

    // 카카오 UUID는 UserEntity에서 가져옵니다.
    @Column(name = "kakao_uuid")
    private String kakaoUuid;  // 카카오 UUID

    @Transient
    private String profileImageBase64;  // 프로필 이미지를 Base64로 변환한 문자열

    // 프로필 이미지를 Base64 형식으로 변환하여 반환
    @JsonProperty("profile_image")
    public String getProfileImageBase64() {
        if (profileImage != null && profileImage.length > 0) {
            String base64 = Base64.getEncoder().encodeToString(profileImage);
            System.out.println("Base64 Encoded Image: " + base64.substring(0, 50) + "...");  // 로그에 Base64 인코딩된 이미지 일부 출력
            return base64;
        }
        System.out.println("profileImage is null or empty");
        return null;  // 이미지가 없을 경우 null 반환
    }

    // 카카오 UUID 반환 (UserEntity에서 가져옴)
    public String getKakaoUuid() {
        return user != null ? user.getKakaoUuid() : null;  // user가 존재하면 kakaoUuid 반환
    }
}