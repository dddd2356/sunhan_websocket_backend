package kakao.login.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "chat_messages")
@Getter
@Setter
public class ChatMessage {
    @Id
    private String id;

    // Change from String to Long to match MySQL ChatRoom ID
    private Long roomId;
    private String senderId;     // employee.user.userId
    private String senderName;   // employee.name
    private String senderDepartment; // employee.department.departmentName
    private String senderPosition;   // employee.position
    private String content;
    private LocalDateTime timestamp;
    private MessageType type = MessageType.CHAT;
    private List<String> readBy = new ArrayList<>();
    private String profileImageBase64; // Base64 encoded profile image
    private boolean exitMessage = false; // 퇴장 메시지 여부
    private boolean inviteMessage = false; // 초대 메시지 여부
    private boolean deleted = false; //메시지 삭제 여부

    // ↓ 추가된 필드 ↓
    /** 첨부 타입: "image" 또는 "file" */
    private String attachmentType;
    /** 저장된 파일 접근용 URL 또는 경로 */
    private String attachmentUrl;
    /** 원본 파일명 */
    private String attachmentName;

    // === 여기에 추가된 필드 ===
    /** 메시지 전송 시점의 활성 참가자 수 스냅샷 */
    private int participantCountAtSend;

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }

    public ChatMessage() {
    }

    public ChatMessage(Long roomId, EmployeeEntity employee, String content) {
        this.roomId = roomId;
        this.senderId = employee.getUser().getUserId();
        this.senderName = employee.getName();
        this.senderDepartment = employee.getDepartment() != null ?
                employee.getDepartment().getDepartmentName() : "";
        this.senderPosition = employee.getPosition();
        this.content = content;
        this.profileImageBase64 = employee.getProfileImageBase64();
        this.timestamp = LocalDateTime.now();
    }

    public void addReadBy(String userId) {
        if (!this.readBy.contains(userId)) {
            this.readBy.add(userId);
        }
    }

    public boolean isSystemMessage() {
        return "시스템".equals(this.senderName);
    }
}