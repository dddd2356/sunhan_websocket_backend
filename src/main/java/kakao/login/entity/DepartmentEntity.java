package kakao.login.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "department")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 증가 ID 사용
    private Long id;

    @Column(name = "department_name", nullable = false, unique = true)  // 데이터베이스에서 컬럼명을 department_name으로 설정

    private String departmentName;  // 부서 이름을 기본 키로 설정

    // 부서 상태를 관리하는 flag 컬럼 (기본값 "add")
    @Column(name = "flag", nullable = false)
    private String flag = "add";

    @OneToMany(mappedBy = "department")
    @JsonManagedReference
    private List<SectionEntity> sections;  // 해당 부서에 속하는 섹션들

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // 무한 참조 방지
    private List<EmployeeEntity> employees;  // 해당 부서에 속한 직원들


    // 필드 값 비교를 위한 equals, hashCode 오버라이드
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepartmentEntity that = (DepartmentEntity) o;
        return Objects.equals(id, that.id);  // id를 기준으로 비교
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);  // id를 기준으로 hashCode 생성
    }
}
