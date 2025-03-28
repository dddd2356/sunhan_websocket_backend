package kakao.login.repository;

import kakao.login.entity.ChatRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, Long> {

    List<ChatRoomParticipant> findByChatRoomId(Long chatRoomId);

    List<ChatRoomParticipant> findByEmployeeId(Long employeeId);

    List<ChatRoomParticipant> findByChatRoomIdAndActiveTrue(Long chatRoomId);

    @Query("SELECT p FROM ChatRoomParticipant p WHERE p.chatRoom.id = :roomId AND p.employee.id = :employeeId")
    Optional<ChatRoomParticipant> findByChatRoomIdAndEmployeeId(
            @Param("roomId") Long roomId,
            @Param("employeeId") Long employeeId);

    @Query("SELECT p FROM ChatRoomParticipant p WHERE p.chatRoom.id = :roomId AND p.employee.user.userId = :userId")
    Optional<ChatRoomParticipant> findByChatRoomIdAndUserId(
            @Param("roomId") Long roomId,
            @Param("userId") String userId);

    @Query("SELECT p FROM ChatRoomParticipant p WHERE p.chatRoom.id = :roomId AND p.employee.user.userId = :userId AND p.active = true")
    Optional<ChatRoomParticipant> findActiveByChatRoomIdAndUserId(
            @Param("roomId") Long roomId,
            @Param("userId") String userId);
}