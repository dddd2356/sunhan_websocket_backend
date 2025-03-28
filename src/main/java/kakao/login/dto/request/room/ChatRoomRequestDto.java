package kakao.login.dto.request.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomRequestDto {
    private Long id;
    private String displayName;   // 이 필드를 프론트에서 사용
    private boolean groupChat;
    private LocalDateTime lastActivity;
    // (필요시 원본 name, participantsCount 등 추가)
}