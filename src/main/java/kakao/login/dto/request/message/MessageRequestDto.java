package kakao.login.dto.request.message;

import lombok.Data;

import java.util.List;

@Data
public class MessageRequestDto {
    // 전송할 메시지 내용
    private String message;

    // 전송 대상 타입 (ALL, DEPARTMENT, INDIVIDUAL)
    private SendType sendType;

    // 전송 대상 부서 ID 리스트 (sendType이 DEPARTMENT인 경우)
    private List<String> departmentIds;

    // 전송 대상 직원 ID 리스트 (sendType이 INDIVIDUAL인 경우)
    private List<String> employeeIds;

    public enum SendType {
        ALL,
        DEPARTMENT,
        INDIVIDUAL
    }

    public void validate() {
        if (sendType == null) {
            throw new IllegalArgumentException("SendType must not be null");
        }
    }
}

