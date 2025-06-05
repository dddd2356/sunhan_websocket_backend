package kakao.login.dto.request.message;

import kakao.login.entity.ChatMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Slf4j
public class ChatMessageRequestDto {
    private String id;
    private String departmentName;
    private String senderId;
    private String senderName;
    private String content;
    private LocalDateTime timestamp;
    private String attachmentType;
    private String attachmentUrl;
    private String attachmentName;
    private List<String> readBy;
    private int unreadCount;
    private boolean deleted;
    private int participantCountAtSend;
    private String status; // "uploading", "done" 등

    public static ChatMessageRequestDto of(ChatMessage msg) {
        ChatMessageRequestDto dto = new ChatMessageRequestDto();
        dto.setId(msg.getId());
        dto.setSenderId(msg.getSenderId());
        dto.setSenderName(msg.getSenderName());
        dto.setContent(msg.getContent());
        dto.setTimestamp(msg.getTimestamp());
        dto.setAttachmentType(msg.getAttachmentType());
        dto.setAttachmentUrl(msg.getAttachmentUrl());
        dto.setAttachmentName(msg.getAttachmentName());
        dto.setReadBy(msg.getReadBy());
        dto.setDeleted(msg.isDeleted());
        dto.setParticipantCountAtSend(msg.getParticipantCountAtSend());
        dto.setStatus(msg.getStatus());

        // unreadCount 계산 (발신자 본인은 읽음 처리)
        Set<String> readBySet = msg.getReadBy() != null ?
                new HashSet<>(msg.getReadBy()) : new HashSet<>();
        readBySet.remove(msg.getSenderId());
        int readCount = readBySet.size();

        // 발신자 제외 계산 (participantCountAtSend - 1)
        int snapshot = msg.getParticipantCountAtSend();
        int unread = Math.max(0, (snapshot - 1) - readCount);
        dto.setUnreadCount(unread);

        // 디버깅 로그
        log.info("Message {}: snapshot={}, readBy={}, readCount={}, unreadCount={}",
                msg.getId(), snapshot, readBySet, readCount, unread);

        return dto;
    }
}
