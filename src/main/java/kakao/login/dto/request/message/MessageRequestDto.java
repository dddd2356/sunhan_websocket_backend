package kakao.login.dto.request.message;

import lombok.Data;
import java.util.List;

@Data
// 메시지 전송 요청 DTO
public class MessageRequestDto {
    private String message;             // 전송할 메시지 내용
    private SendType sendType;          // 전송 대상 타입 (ALL, DEPARTMENT, INDIVIDUAL)
    private List<String> departmentIds; // 전송 대상 부서 ID 리스트 (sendType이 DEPARTMENT인 경우)
    private List<String> employeeIds;   // 전송 대상 직원 ID 리스트 (sendType이 INDIVIDUAL인 경우)

    // 메시지 전송 대상 타입을 정의하는 Enum
    public enum SendType {
        ALL,        // 전체 전송
        DEPARTMENT, // 부서별 전송
        INDIVIDUAL  // 개인별 전송
    }

    // 유효성 검증 메서드 (sendType이 null일 경우 예외 처리)
    public void validate() {
        if (sendType == null) {
            throw new IllegalArgumentException("SendType must not be null");
        }
    }
}