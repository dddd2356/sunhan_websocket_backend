package kakao.login.repository;

import kakao.login.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByNameContaining(String name);

    Optional<ChatRoom> findByName(String name);

    // Find rooms by participant ID with active participation
    @Query("SELECT DISTINCT cr FROM ChatRoom cr JOIN cr.chatRoomParticipants p " +
            "WHERE p.employee.id = :employeeId AND p.active = true")
    List<ChatRoom> findByParticipantId(@Param("employeeId") Long employeeId);

    // Find rooms by participant userId with active participation
    @Query("SELECT DISTINCT cr FROM ChatRoom cr JOIN cr.chatRoomParticipants p " +
            "WHERE p.employee.user.userId = :userId AND p.active = true")
    List<ChatRoom> findByParticipantUserId(@Param("userId") String userId);

    // Find room IDs for a user with active participation
    @Query("SELECT cr.id FROM ChatRoom cr JOIN cr.chatRoomParticipants p " +
            "WHERE p.employee.user.userId = :userId AND p.active = true")
    List<Long> findRoomIdsByUserId(@Param("userId") String userId);

    // Find direct chat room between two participants
    @Query("SELECT r " +
            "FROM ChatRoom r " +
            " JOIN r.chatRoomParticipants p1 " +
            " JOIN r.chatRoomParticipants p2 " +
            "WHERE r.isGroupChat = false " +
            "  AND p1.employee.id = :user1Id " +
            "  AND p2.employee.id = :user2Id " +
            "  AND SIZE(r.chatRoomParticipants) = 2")
    Optional<ChatRoom> findDirectChatRoomByParticipantIds(@Param("user1Id") Long user1Id,
                                                          @Param("user2Id") Long user2Id);

    // Find direct chat room between two participants by userId
    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.isGroupChat = false " +
            "AND EXISTS (SELECT p1 FROM ChatRoomParticipant p1 WHERE p1.chatRoom = cr AND p1.employee.user.userId = :user1Id) " +
            "AND EXISTS (SELECT p2 FROM ChatRoomParticipant p2 WHERE p2.chatRoom = cr AND p2.employee.user.userId = :user2Id)")
    Optional<ChatRoom> findDirectChatRoom(
            @Param("user1Id") String user1Id,
            @Param("user2Id") String user2Id);

    // Find rooms by department
    @Query("SELECT DISTINCT cr FROM ChatRoom cr JOIN cr.chatRoomParticipants p " +
            "JOIN p.employee e JOIN e.department d " +
            "WHERE d.departmentName = :departmentName AND p.active = true")
    List<ChatRoom> findByDepartmentName(@Param("departmentName") String departmentName);

    // Find rooms by department and section
    @Query("SELECT DISTINCT cr FROM ChatRoom cr JOIN cr.chatRoomParticipants p " +
            "JOIN p.employee e JOIN e.department d JOIN e.section s " +
            "WHERE d.departmentName = :departmentName AND s.sectionName = :sectionName AND p.active = true")
    List<ChatRoom> findByDepartmentAndSection(
            @Param("departmentName") String departmentName,
            @Param("sectionName") String sectionName);


}