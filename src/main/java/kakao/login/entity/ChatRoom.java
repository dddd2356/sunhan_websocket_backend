package kakao.login.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@Slf4j
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @Column(name = "is_group_chat")
    private boolean isGroupChat;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<ChatRoomParticipant> chatRoomParticipants = new HashSet<>();


    public ChatRoom() {
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    public ChatRoom(String name, String creatorId, boolean isGroupChat) {
        this.name = name;
        this.createdBy = creatorId;
        this.isGroupChat = isGroupChat;
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    public void addParticipant(EmployeeEntity employee, boolean rejoin) {
        Optional<ChatRoomParticipant> existingParticipant = chatRoomParticipants.stream()
                .filter(p -> p.getEmployee().getId().equals(employee.getId()))
                .findFirst();

        ChatRoomParticipant participant;
        if (existingParticipant.isPresent()) {
            participant = existingParticipant.get();
            if (rejoin && participant.getLastLeftAt() != null && !participant.isActive()) {
                participant.rejoin();
                log.info("Rejoining participant {} to room {}", employee.getUser().getUserId(), id);
            } else {
                log.info("Participant {} is already active or no rejoin needed in room {}", employee.getUser().getUserId(), id);
            }
        } else {
            participant = new ChatRoomParticipant(this, employee);
            chatRoomParticipants.add(participant);
            log.info("Added new participant {} to room {}", employee.getUser().getUserId(), id);
        }
    }

    public void removeParticipant(EmployeeEntity employee) {
        chatRoomParticipants.stream()
                .filter(p -> p.getEmployee().getId().equals(employee.getId()))
                .findFirst()
                .ifPresent(participant -> {
                    participant.leave();
                    log.info("Removed participant {} from room {}", employee.getUser().getUserId(), id);
                });
    }

    @Transactional(readOnly = true)
    public boolean hasActiveParticipant(String userId) {
        if (userId == null) {
            log.warn("userId가 null입니다.");
            return false;
        }
        return chatRoomParticipants.stream()
                .anyMatch(participant ->
                        participant.isActive() &&
                                participant.getEmployee().getUser() != null &&
                                userId.equals(participant.getEmployee().getUser().getUserId()));
    }

    public Set<EmployeeEntity> getActiveParticipants() {
        return chatRoomParticipants.stream()
                .filter(ChatRoomParticipant::isActive)
                .map(ChatRoomParticipant::getEmployee)
                .collect(java.util.stream.Collectors.toSet());
    }

    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    @Transient
    public String getDisplayNameFor(String currentUserId) {
        // 그룹 채팅이면 name 그대로
        if (this.isGroupChat) {
            return this.name;
        }

        // 1:1 채팅일 때 - 상대방 이름 반환
        return chatRoomParticipants.stream()
                .map(ChatRoomParticipant::getEmployee)
                .filter(emp -> emp.getUser() != null)
                .filter(emp -> !currentUserId.equals(emp.getUser().getUserId()))
                .map(EmployeeEntity::getName)
                .findFirst()
                .orElse("알 수 없음");
    }
    @Transactional(readOnly = true)
    public boolean hasParticipantLeft(Long employeeId) {
        return chatRoomParticipants.stream()
                .filter(p -> p.getEmployee().getId().equals(employeeId))
                .anyMatch(p -> p.getLastLeftAt() != null && !p.isActive());
    }
}