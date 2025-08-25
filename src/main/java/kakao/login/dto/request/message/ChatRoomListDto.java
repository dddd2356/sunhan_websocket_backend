package kakao.login.dto.request.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import kakao.login.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomListDto {
    private Long id;
    private String name;
    private String displayName;
    private boolean isGroupChat;
    private LocalDateTime lastActivity;
    @JsonProperty("lastMessage")
    private String lastMessageContent;
    private long unreadCount;
    private int activeParticipantCount;

    public static ChatRoomListDto of(ChatRoom chatRoom, String currentUserId, long unreadCount) {
        ChatRoomListDto dto = new ChatRoomListDto();
        dto.setId(chatRoom.getId());
        dto.setName(chatRoom.getName());
        dto.setDisplayName(chatRoom.getDisplayNameFor(currentUserId));
        dto.setGroupChat(chatRoom.isGroupChat());
        dto.setLastActivity(chatRoom.getLastActivity());
        dto.setLastMessageContent(chatRoom.getLastMessageContent());
        dto.setUnreadCount(unreadCount);
        dto.setActiveParticipantCount(chatRoom.getActiveParticipants().size());
        return dto;
    }

}