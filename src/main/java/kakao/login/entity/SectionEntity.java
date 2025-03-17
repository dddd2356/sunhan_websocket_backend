package kakao.login.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import kakao.login.entity.DepartmentEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "section")  // "section" 테이블과 매핑되는 엔티티 클래스
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 자동 증가하는 ID
    private Long id;

    @Column(name = "section_name")  // "section_name" 컬럼과 매핑
    private String sectionName;  // 섹션 이름을 PK로 사용

    @ManyToOne  // 다대일 관계: 여러 섹션은 하나의 부서에 속함
    @JoinColumn(name = "department_id", referencedColumnName = "id")  // department_id 외래 키
    @JsonBackReference  // 직렬화 시 무한 참조 방지 (부서 엔티티에서 참조될 때 JSON에서 무시됨)
    private DepartmentEntity department;  // 부서 참조
}
