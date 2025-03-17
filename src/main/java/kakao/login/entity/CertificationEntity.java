package kakao.login.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name="certification")
@Table(name="certification")
// 사용자 인증 관련 데이터를 저장하는 엔티티
public class CertificationEntity {

    @Id
    private String userId;  // 사용자 ID
    private String email;    // 사용자 이메일
    private String certificationNumber;  // 인증 번호
}
