package kakao.login.service;

import kakao.login.dto.request.room.ChatRoomRequestDto;
import kakao.login.entity.*;
import kakao.login.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatRoomService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ChatRoomParticipantRepository participantRepository;

    @Transactional
    public ChatRoom getOrCreateDirectChatRoom(String user1Id, String user2Id) {
        UserEntity user1Entity = userRepository.findByUserId(user1Id);
        if (user1Entity == null) {
            log.warn("No employee record found for userId: {}", user1Id);
            throw new IllegalStateException("Cannot create chat room: user " + user1Id + " is not an employee");
        }
        UserEntity user2Entity = userRepository.findByUserId(user2Id);
        if (user2Entity == null) {
            log.warn("No employee record found for userId: {}", user2Id);
            throw new IllegalStateException("Cannot create chat room: user " + user2Id + " is not an employee");
        }

        EmployeeEntity user1 = employeeRepository.findByUser_UserId(user1Id);
        if (user1 == null) {
            log.warn("No employee record found for userId: {}", user1Id);
            throw new IllegalStateException("Cannot create chat room: user " + user1Id + " is not an employee");
        }
        EmployeeEntity user2 = employeeRepository.findByUser_UserId(user2Id);
        if (user2 == null) {
            log.warn("No employee record found for userId: {}", user2Id);
            throw new IllegalStateException("Cannot create chat room: user " + user2Id + " is not an employee");
        }

        Long user1Pk = user1.getId();
        Long user2Pk = user2.getId();

        // ① 순서대로 조회
        Optional<ChatRoom> directRoomOpt = chatRoomRepository.findDirectChatRoomByParticipantIds(user1Pk, user2Pk);

        // ② 순서 바꿔 조회 (양방향 동일 처리)
        if (directRoomOpt.isEmpty()) {
            directRoomOpt = chatRoomRepository.findDirectChatRoomByParticipantIds(user2Pk, user1Pk);
        }

        if (directRoomOpt.isPresent()) {
            ChatRoom existingRoom = directRoomOpt.get();
            // 재가입 필요 여부 확인
            boolean user1NeedsRejoin = existingRoom.hasParticipantLeft(user1Pk);
            boolean user2NeedsRejoin = existingRoom.hasParticipantLeft(user2Pk);
            existingRoom.addParticipant(user1, user1NeedsRejoin);
            existingRoom.addParticipant(user2, user2NeedsRejoin);
            existingRoom.updateLastActivity();
            return chatRoomRepository.save(existingRoom);
        }

        // 방이 없으면 새로 생성
        ChatRoom newRoom = new ChatRoom("DirectChat", user1Id, false);
        newRoom.addParticipant(user1, false);
        newRoom.addParticipant(user2, false);
        newRoom.updateLastActivity();
        return chatRoomRepository.save(newRoom);
    }


    @Transactional
    public ChatRoom createGroupChatRoom(String name, List<String> participantIds, String creatorId) {
        ChatRoom chatRoom = new ChatRoom(name, creatorId, true);
        chatRoomRepository.save(chatRoom);
        for (String participantId : participantIds) {
            Long pk = Long.parseLong(participantId);
            EmployeeEntity employee = employeeRepository.findById(pk)
                    .orElseThrow(() -> new RuntimeException("Employee not found with id: " + participantId));
            chatRoom.addParticipant(employee, false);
        }
        return chatRoomRepository.save(chatRoom);
    }

    public Optional<ChatRoom> getMainChatRoom() {
        List<ChatRoom> mainRooms = chatRoomRepository.findByNameContaining("Main Room");
        if (!mainRooms.isEmpty()) {
            return Optional.of(mainRooms.get(0));
        } else {
            ChatRoom mainRoom = createChatRoom("Main Room", "system", true);
            return Optional.of(mainRoom);
        }
    }

    @Transactional
    public ChatRoom joinMainChatRoom(EmployeeEntity employee) {
        ChatRoom mainRoom = getMainChatRoom().orElseThrow(() -> new RuntimeException("Main chat room creation failed"));
        if (!mainRoom.hasActiveParticipant(employee.getUser().getUserId())) {
            mainRoom.addParticipant(employee, false);
            mainRoom.updateLastActivity();
            mainRoom = chatRoomRepository.save(mainRoom);
        }
        return mainRoom;
    }

    @Transactional
    public ChatRoom createChatRoom(String name, String creatorId, boolean isGroupChat) {
        ChatRoom chatRoom = new ChatRoom(name, creatorId, isGroupChat);
        return chatRoomRepository.save(chatRoom);
    }

    public List<ChatRoom> getUserChatRooms(String userId) {
        EmployeeEntity employee = employeeRepository.findByUser_UserId(userId);
        if (employee == null) {
            throw new RuntimeException("Employee not found for userId: " + userId);
        }
        Long empId = employee.getId();
        return chatRoomRepository.findByParticipantId(empId);
    }

    public Optional<ChatRoom> getChatRoom(Long roomId) {
        return chatRoomRepository.findById(roomId);
    }

    public List<Long> getUserRoomIds(String userId) {
        return chatRoomRepository.findRoomIdsByUserId(userId);
    }

    @Transactional
    public ChatRoom addUserToChatRoom(Long roomId, String userId) {
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            ChatRoom room = roomOpt.get();
            EmployeeEntity employee = employeeRepository.findByUser_UserId(userId);
            if (employee == null) {
                log.error("Employee not found with userId: {}", userId);
                throw new RuntimeException("Employee not found with userId: " + userId);
            }
            room.addParticipant(employee, true); // Allow rejoin for invites
            room.updateLastActivity();
            log.info("User {} added/invited to chat room {}", userId, roomId);
            return chatRoomRepository.save(room);
        }
        log.error("Chat room not found with id: {}", roomId);
        throw new RuntimeException("Chat room not found with id: " + roomId);
    }

    @Transactional
    public ChatRoom removeUserFromChatRoom(Long roomId, String userId) {
        log.info("Attempting to remove user {} from chat room {}", userId, roomId);
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            log.error("Chat room not found: {}", roomId);
            throw new RuntimeException("Chat room not found with id: " + roomId);
        }

        ChatRoom room = roomOpt.get();
        EmployeeEntity employee = employeeRepository.findByUser_UserId(userId);
        if (employee == null) {
            log.error("Employee not found with userId: {}", userId);
            throw new RuntimeException("Employee not found with userId: " + userId);
        }

        if (!room.hasActiveParticipant(userId)) {
            log.warn("User {} is not an active participant in room {}", userId, roomId);
        }

        room.removeParticipant(employee);
        room.updateLastActivity();

        Set<EmployeeEntity> activeParticipants = room.getActiveParticipants();
        if (activeParticipants.isEmpty()) {
            log.info("No active participants left in room {}, deleting room", roomId);
            chatRoomRepository.delete(room);
            return null;
        }

        log.info("User {} successfully removed from chat room {}", userId, roomId);
        return chatRoomRepository.save(room);
    }

    public void updateRoomActivity(Long roomId) {
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(roomId);
        roomOpt.ifPresent(room -> {
            room.setLastActivity(LocalDateTime.now());
            chatRoomRepository.save(room);
        });
    }

    public void deleteChatRoom(Long roomId) {
        chatRoomRepository.deleteById(roomId);
    }

    public Set<String> getChatRoomParticipants(Long roomId) {
        Optional<ChatRoom> chatRoom = chatRoomRepository.findById(roomId);
        return chatRoom.map(room ->
                room.getActiveParticipants().stream()
                        .map(employee -> employee.getUser().getUserId())
                        .collect(Collectors.toSet())
        ).orElse(Collections.emptySet());
    }
    public ChatRoomRequestDto toDto(ChatRoom room, String currentUserId) {
        return ChatRoomRequestDto.builder()
                .id(room.getId())
                .displayName(room.getDisplayNameFor(currentUserId))
                .groupChat(room.isGroupChat())
                .lastActivity(room.getLastActivity())
                // 필요 시 participantsCount, unreadCount 등 추가 필드 설정
                .build();
    }

    @Transactional
    public ChatRoom getOrCreateDirectChatRoomByEmployeeId(Long employee1Id, Long employee2Id) {
        // Find employees by employee_id
        EmployeeEntity employee1 = employeeRepository.findById(employee1Id)
                .orElseThrow(() -> {
                    log.warn("No employee record found for employeeId: {}", employee1Id);
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee with ID " + employee1Id + " not found");
                });
        EmployeeEntity employee2 = employeeRepository.findById(employee2Id)
                .orElseThrow(() -> {
                    log.warn("No employee record found for employeeId: {}", employee2Id);
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee with ID " + employee2Id + " not found");
                });

        // Get user_id values
        String user1Id = employee1.getUser().getUserId();
        String user2Id = employee2.getUser().getUserId();

        // Check for existing chat room
        Optional<ChatRoom> directRoomOpt = chatRoomRepository.findDirectChatRoomByParticipantIds(employee1Id, employee2Id);
        if (directRoomOpt.isEmpty()) {
            directRoomOpt = chatRoomRepository.findDirectChatRoomByParticipantIds(employee2Id, employee1Id);
        }

        if (directRoomOpt.isPresent()) {
            ChatRoom existingRoom = directRoomOpt.get();
            boolean user1NeedsRejoin = existingRoom.hasParticipantLeft(employee1Id);
            boolean user2NeedsRejoin = existingRoom.hasParticipantLeft(employee2Id);
            existingRoom.addParticipant(employee1, user1NeedsRejoin);
            existingRoom.addParticipant(employee2, user2NeedsRejoin);
            existingRoom.updateLastActivity();
            return chatRoomRepository.save(existingRoom);
        }

        // Create new chat room
        ChatRoom newRoom = new ChatRoom("DirectChat", user1Id, false);
        newRoom.addParticipant(employee1, false);
        newRoom.addParticipant(employee2, false);
        newRoom.updateLastActivity();
        return chatRoomRepository.save(newRoom);
    }
}