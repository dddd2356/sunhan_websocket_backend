package kakao.login.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room_participants")
@Getter
@Setter
public class ChatRoomParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_room_id")
    @JsonBackReference
    private ChatRoom chatRoom;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private EmployeeEntity employee;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "last_left_at")
    private LocalDateTime lastLeftAt;

    @Column(name = "is_active")
    private boolean active = true;

    // 기본 생성자
    public ChatRoomParticipant() {
        this.joinedAt = LocalDateTime.now();
    }

    // 편의 생성자
    public ChatRoomParticipant(ChatRoom chatRoom, EmployeeEntity employee) {
        this.chatRoom = chatRoom;
        this.employee = employee;
        this.joinedAt = LocalDateTime.now();
        this.active = true;
    }

    // 사용자가 채팅방을 나갈 때 호출
    public void leave() {
        this.active = false;
        this.lastLeftAt = LocalDateTime.now();
    }

    // 사용자가 채팅방에 다시 들어올 때 호출
    public void rejoin() {
        this.active = true;
        this.joinedAt = LocalDateTime.now();
    }
}