package kakao.login.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
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
public class EmployeeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 별도의 id 필드 추가
    private Long id; // 새로운 ID 추가

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    private String name;
    private String phone;

    @ManyToOne
    @JoinColumn(name = "department_id", referencedColumnName = "id")  // department_id 외래 키로 연결
    @JsonIgnore // 무한 참조 방지
    private DepartmentEntity department;  // 부서 (DepartmentEntity 참조)

    @ManyToOne
    @JoinColumn(name = "section_id", referencedColumnName = "id")  // section_id 외래 키로 연결
    private SectionEntity section;  // 섹션 (SectionEntity 참조)


    private String position;  // 직급

    @Lob
    @JsonIgnore // 원본 byte[] 데이터는 JSON에 포함되지 않도록 처리
    @Column(name = "profile_image")
    private byte[] profileImage;

    @Transient
    private String profileImageBase64;


    @JsonProperty("profile_image")
    public String getProfileImageBase64() {
        if (profileImage != null && profileImage.length > 0) {
            String base64 = Base64.getEncoder().encodeToString(profileImage);
            System.out.println("Base64 Encoded Image: " + base64.substring(0, 50) + "...");  // 앞 50글자만 출력
            return base64;
        }
        System.out.println("profileImage is null or empty");
        return null;
    }

}
