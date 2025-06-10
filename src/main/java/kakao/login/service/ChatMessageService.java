package kakao.login.service;

import kakao.login.dto.request.message.ChatMessageRequestDto;
import kakao.login.dto.request.message.ChatRoomListDto;
import kakao.login.entity.*;
import kakao.login.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatMessageService {

    private static final String SYSTEM_SENDER_ID = "SYSTEM";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final EmployeeRepository employeeRepository;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket 메시지 전송용

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ChatRoomParticipantRepository participantRepository;

    @Autowired
    public ChatMessageService(ChatRoomRepository chatRoomRepository,
                              ChatMessageRepository chatMessageRepository,
                              EmployeeRepository employeeRepository, SimpMessagingTemplate messagingTemplate) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.employeeRepository = employeeRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Page<ChatMessage> getMessages(Long roomId, Pageable pageable, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (!chatRoom.hasActiveParticipant(userId)) {
            log.warn("User {} is not an active participant in room {}", userId, roomId);
            return Page.empty(pageable);
        }

        Optional<ChatRoomParticipant> participantOpt = chatRoom.getChatRoomParticipants().stream()
                .filter(p -> p.getEmployee().getUser().getUserId().equals(userId))
                .findFirst();

        if (participantOpt.isEmpty()) {
            log.warn("Participant record not found for user {} in room {}", userId, roomId);
            return Page.empty(pageable);
        }

        ChatRoomParticipant participant = participantOpt.get();
        LocalDateTime joinedAt = participant.getJoinedAt();
        LocalDateTime lastLeftAt = participant.getLastLeftAt();
        LocalDateTime roomCreatedAt = chatRoom.getCreatedAt();

        // 메시지 조회
        List<ChatMessage> allMessages = chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId);
        List<ChatMessage> filteredMessages;

        if (lastLeftAt == null || joinedAt.isBefore(roomCreatedAt)) {
            // 나간 적 없거나 joinedAt이 채팅방 생성 시점보다 이전인 경우: 모든 메시지
            filteredMessages = allMessages.stream()
                    .filter(msg -> msg.getTimestamp().isAfter(roomCreatedAt))
                    .collect(Collectors.toList());
            log.info("User {} has never left room {} or joinedAt is old, returning all messages since room creation", userId, roomId);
        } else {
            // 나간 적 있는 경우: joinedAt 이후 메시지
            filteredMessages = allMessages.stream()
                    .filter(msg -> msg.getTimestamp().isAfter(joinedAt))
                    .collect(Collectors.toList());
            log.info("User {} left room {} at {}, returning messages after joinedAt {}", userId, roomId, lastLeftAt, joinedAt);
        }

        // 읽음 처리 (재진입 시 읽지 않은 메시지만 처리)
        filteredMessages.forEach(msg -> {
            if (!userId.equals(msg.getSenderId()) && (msg.getReadBy() == null || !msg.getReadBy().contains(userId))) {
                markMessagesAsRead(msg.getId(), userId);
            }
        });

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredMessages.size());

        if (start >= filteredMessages.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, filteredMessages.size());
        }

        List<ChatMessage> pagedMessages = filteredMessages.subList(start, end);
        return new PageImpl<>(pagedMessages, pageable, filteredMessages.size());
    }

    @Transactional
    public ChatRoom createChatRoom(String name, String creatorId, boolean isGroupChat) {
        EmployeeEntity creator = employeeRepository.findByUser_UserId(creatorId);
        if (creator == null) {
            throw new RuntimeException("Employee not found with userId: " + creatorId);
        }
        ChatRoom chatRoom = new ChatRoom(name, creatorId, isGroupChat);
        chatRoomRepository.save(chatRoom);
        chatRoom.addParticipant(creator, false);
        // 🔥 추가: 새로운 채팅방 생성 시 lastMessageContent를 초기화
        chatRoom.setLastMessageContent("채팅방이 생성되었습니다.");
        return chatRoomRepository.save(chatRoom);
    }

    public List<EmployeeEntity> getParticipants(Long roomId) {
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(roomId);
        if (chatRoomOpt.isEmpty()) {
            throw new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId);
        }
        ChatRoom chatRoom = chatRoomOpt.get();
        return new ArrayList<>(chatRoom.getActiveParticipants());
    }

    @Transactional
    public ChatRoom addParticipant(Long roomId, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        EmployeeEntity employee = employeeRepository.findByUser_UserId(userId);
        if (employee == null) {
            throw new RuntimeException("Employee not found with userId: " + userId);
        }
        chatRoom.addParticipant(employee, true); // 명시적 추가 시 재입장 허용
        chatRoom.updateLastActivity();
        return chatRoomRepository.save(chatRoom);
    }

    public boolean isParticipant(Long roomId, String userId) {
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(roomId);
        if (chatRoomOpt.isEmpty()) {
            return false;
        }
        ChatRoom chatRoom = chatRoomOpt.get();
        return chatRoom.hasActiveParticipant(userId);
    }

    @Transactional
    public ChatRoom removeParticipant(Long roomId, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        EmployeeEntity employee = employeeRepository.findByUser_UserId(userId);
        if (employee == null) {
            throw new RuntimeException("Employee not found with userId: " + userId);
        }
        chatRoom.removeParticipant(employee);
        chatRoom.updateLastActivity();
        log.info("User {} left chat room {}", userId, roomId);
        return chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public ChatMessage sendMessage(ChatMessage message) {
        log.info("메시지 전송 시도: roomId={}, userId={}, message={}",
                message.getRoomId(), message.getSenderId(), message.getContent());

        ChatRoom chatRoom = chatRoomRepository.findById(message.getRoomId())
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        // 날짜 구분 시스템 메시지 추가
        insertDateSeparatorIfNeeded(chatRoom.getId());

        Hibernate.initialize(chatRoom.getChatRoomParticipants());

        // 자신을 제외한 활성 참가자 수 계산
        int activeParticipantsCount = (int) chatRoom.getActiveParticipants().stream()
                .filter(p -> !p.getUser().getUserId().equals(message.getSenderId()))
                .count();
        message.setParticipantCountAtSend(activeParticipantsCount);

        EmployeeEntity sender = employeeRepository.findByUser_UserId(message.getSenderId());
        if (sender == null) {
            throw new RuntimeException("Sender not found with userId: " + message.getSenderId());
        }
        if (!chatRoom.hasActiveParticipant(message.getSenderId())) {
            log.info("발신자 {}는 채팅방 {}의 참가자가 아니므로 추가합니다.", message.getSenderId(), message.getRoomId());
            chatRoom.addParticipant(sender, true);
        }

        if (message.getReadBy() == null) {
            message.setReadBy(new ArrayList<>());
        }
        message.getReadBy().add(message.getSenderId());

        ChatMessage saved = chatMessageRepository.save(message);

        // 🔥 수정: attachmentType에 따라 이모지+문구로 lastMessageContent 설정
        String attachmentType = message.getAttachmentType();
        String lastMsgContent;
        if ("image".equalsIgnoreCase(attachmentType)) {
            lastMsgContent = "📷 사진";
        } else if ("file".equalsIgnoreCase(attachmentType)) {
            lastMsgContent = "📄 파일";
        } else {
            lastMsgContent = message.getContent();
        }
        chatRoom.setLastMessageContent(lastMsgContent);

        chatRoom.updateLastActivity();
        chatRoomRepository.save(chatRoom);

        ChatMessageRequestDto dto = ChatMessageRequestDto.of(saved);
        messagingTemplate.convertAndSend("/topic/chat/" + saved.getRoomId(), dto);
        log.info("Broadcasted new message {} to /topic/chat/{}", saved.getId(), saved.getRoomId());

        // --- unread-count 브로드캐스트 (lastMessageContent에도 동일한 값 사용) ---
        ChatRoom room = chatRoomRepository.findById(saved.getRoomId()).orElseThrow();
        Map<String, Long> unreadCounts = room.getActiveParticipants().stream()
                .map(p -> p.getUser().getUserId())
                .collect(Collectors.toMap(
                        Function.identity(),
                        uid -> getUnreadCount(saved.getRoomId(), uid)
                ));
        messagingTemplate.convertAndSend(
                "/topic/chat/" + saved.getRoomId() + "/unread-count",
                Map.of(
                        "unreadCounts", unreadCounts,
                        "lastMessageContent", lastMsgContent
                )
        );

        return saved;
    }


    @Transactional
    public ChatMessage sendDirectMessage(Long roomId, String senderId, String content, boolean invite) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (chatRoom.isGroupChat()) {
            throw new IllegalArgumentException("This method is for direct messaging only");
        }

        // 날짜 구분 시스템 메시지 추가
        insertDateSeparatorIfNeeded(roomId);

        // participantCountAtSend 세팅: 나를 제외한 활성 참가자 수 (1대1이면 항상 1)
        Hibernate.initialize(chatRoom.getChatRoomParticipants());
        int activeParticipantsCount = (int) chatRoom.getActiveParticipants().stream()
                .filter(p -> !p.getUser().getUserId().equals(senderId))
                .count();

        EmployeeEntity sender = employeeRepository.findByUser_UserId(senderId);
        if (sender == null) {
            throw new RuntimeException("Sender not found with userId: " + senderId);
        }

        if (!chatRoom.hasActiveParticipant(senderId)) {
            log.info("발신자 {}를 채팅방 {}에 다시 추가합니다", senderId, roomId);
            chatRoom.addParticipant(sender, true); // 재입장 허용
        }

        if (invite) {
            List<ChatMessage> previousMessages = chatMessageRepository.findTop50ByRoomIdOrderByTimestampDesc(roomId);
            List<String> allUserIds = previousMessages.stream()
                    .map(ChatMessage::getSenderId)
                    .distinct()
                    .collect(Collectors.toList());

            List<String> recipientIds = allUserIds.stream()
                    .filter(id -> !id.equals(senderId))
                    .collect(Collectors.toList());

            for (String recipientId : recipientIds) {
                EmployeeEntity recipient = employeeRepository.findByUser_UserId(recipientId);
                if (recipient != null && !chatRoom.hasActiveParticipant(recipientId)) {
                    log.info("대화 상대 {}를 채팅방 {}에 다시 추가합니다", recipientId, roomId);
                    chatRoom.addParticipant(recipient, true); // 재입장 허용
                }
            }
            ChatMessage inviteMessage = new ChatMessage(roomId, sender, content);
            inviteMessage.setParticipantCountAtSend(activeParticipantsCount);
            inviteMessage.setInviteMessage(true);
            ChatMessage savedMessage = chatMessageRepository.save(inviteMessage);

            // 🔥 초대 메시지도 lastMessageContent 업데이트 (필요에 따라)
            chatRoom.setLastMessageContent(inviteMessage.getContent());
            chatRoom.updateLastActivity();
            chatRoomRepository.save(chatRoom);
            log.info("Invite event: 초대 메시지는 저장하지 않고 로그에만 남김 (roomId={}, senderId={})", roomId, senderId);
            // 브로드캐스트
            ChatMessageRequestDto dto = ChatMessageRequestDto.of(savedMessage);
            messagingTemplate.convertAndSend("/topic/chat/" + roomId, dto);
            // 🔥 unread-count 브로드캐스트에 lastMessageContent 포함
            ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
            Map<String, Long> unreadCounts = room.getActiveParticipants().stream()
                    .map(p -> p.getUser().getUserId())
                    .collect(Collectors.toMap(Function.identity(), uid -> getUnreadCount(roomId, uid)));
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + roomId + "/unread-count",
                    Map.of(
                            "unreadCounts", unreadCounts,
                            "lastMessageContent", inviteMessage.getContent() // 🔥 추가
                    )
            );
            return savedMessage;
        } else {
            ChatMessage message = new ChatMessage(roomId, sender, content);

            message.setParticipantCountAtSend(activeParticipantsCount);
            // 발신자는 이미 읽은 것으로 처리
            if (message.getReadBy() == null) {
                message.setReadBy(new ArrayList<>());
            }
            message.getReadBy().add(senderId);

            ChatMessage savedMessage = chatMessageRepository.save(message);
            // 🔥 추가: ChatRoom의 lastMessageContent 업데이트
            chatRoom.setLastMessageContent(message.getContent());
            chatRoom.updateLastActivity();
            chatRoomRepository.save(chatRoom);
            // DTO 변환 후 브로드캐스트
            ChatMessageRequestDto dto = ChatMessageRequestDto.of(savedMessage);
            messagingTemplate.convertAndSend("/topic/chat/" + roomId, dto);
            // 🔥 unread-count 브로드캐스트에 lastMessageContent 포함
            ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
            Map<String, Long> unreadCounts = room.getActiveParticipants().stream()
                    .map(p -> p.getUser().getUserId())
                    .collect(Collectors.toMap(Function.identity(), uid -> getUnreadCount(roomId, uid)));
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + roomId + "/unread-count",
                    Map.of(
                            "unreadCounts", unreadCounts,
                            "lastMessageContent", message.getContent() // 🔥 추가
                    )
            );
            return savedMessage;
        }
    }

    public ChatMessage createMessageFromRequest(Long roomId, String userId, String content) {
        EmployeeEntity employee = employeeRepository.findByUser_UserId(userId);
        if (employee == null) {
            throw new RuntimeException("Employee not found with userId: " + userId);
        }
        log.info("메시지 생성: employee.getUser().getUserId() = {}", employee.getUser().getUserId());
        return new ChatMessage(roomId, employee, content);
    }

    public List<ChatRoom> getUserChatRooms(String userId) {
        return chatRoomRepository.findByParticipantUserId(userId);
    }

    @Transactional
    public void markMessagesAsRead(String messageId, String userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        log.info("Marking message as read: messageId={}, userId={}, exitMessage={}, inviteMessage={}",
                messageId, userId, message.isExitMessage(), message.isInviteMessage());

        if (message.getReadBy() == null) {
            message.setReadBy(new ArrayList<>());
        }
        if (!message.getReadBy().contains(userId) && !userId.equals(message.getSenderId())) {
            message.getReadBy().add(userId);
            ChatMessage savedMessage = chatMessageRepository.save(message);
            log.info("Message {} marked as read by user {}. New readBy: {}",
                    messageId, userId, savedMessage.getReadBy());

            // 1) 현재 ChatRoom 조회
            ChatRoom room = chatRoomRepository.findById(savedMessage.getRoomId())
                    .orElseThrow();
            // 2) 읽지 않은 메시지 개수를 모든 참가자 대상으로 계산
            Map<String, Long> unreadCounts = room.getActiveParticipants().stream()
                    .map(p -> p.getUser().getUserId())
                    .collect(Collectors.toMap(
                            Function.identity(),
                            uid -> getUnreadCount(savedMessage.getRoomId(), uid)
                    ));

            // 3) lastMessageContent 가져오기 (null-safe)
            String lastMsg = room.getLastMessageContent() != null
                    ? room.getLastMessageContent()
                    : "";

            // 4) '/topic/chat/{roomId}/unread-count' 브로드캐스트 (lastMessageContent 포함)
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("unreadCounts", unreadCounts);
            payload.put("lastMessageContent", lastMsg);

            messagingTemplate.convertAndSend(
                    "/topic/chat/" + savedMessage.getRoomId() + "/unread-count",
                    payload
            );
            log.info("Broadcasted unreadCounts for room {} after single message read: {} with lastMessageContent={}",
                    savedMessage.getRoomId(), unreadCounts, lastMsg);
        } else {
            log.info("Message {} already read by user {} or is sender", messageId, userId);
        }
    }

    public Optional<ChatRoom> findDirectChatRoom(String user1Id, String user2Id) {
        return chatRoomRepository.findDirectChatRoom(user1Id, user2Id);
    }

    public Optional<ChatRoom> getChatRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId);
    }

    public List<ChatRoom> getChatRoomsByDepartment(String departmentName) {
        return chatRoomRepository.findByDepartmentName(departmentName);
    }

    public List<ChatRoom> getChatRoomsByDepartmentAndSection(String departmentName, String sectionName) {
        return chatRoomRepository.findByDepartmentAndSection(departmentName, sectionName);
    }

    public Optional<ChatRoom> getMainChatRoom() {
        Optional<ChatRoom> mainRoom = chatRoomRepository.findByName("메인 채팅방");
        if (mainRoom.isEmpty()) {
            ChatRoom newRoom = new ChatRoom();
            newRoom.setName("메인 채팅방");
            newRoom.setGroupChat(true);
            newRoom.setCreatedBy("admin");
            // 🔥 추가: 메인 채팅방 생성 시 lastMessageContent 초기화
            newRoom.setLastMessageContent("메인 채팅방이 생성되었습니다.");
            chatRoomRepository.save(newRoom);
            return Optional.of(newRoom);
        }
        return mainRoom;
    }

    public ChatMessage createSystemMessage(Long roomId, String senderId, String content) {
        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setSenderId(senderId);
        message.setSenderName("시스템");
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());

        // 시스템 메시지에 대해 readBy를 초기화
        message.setReadBy(new ArrayList<>()); // 시스템 메시지 읽음 처리 리스트 초기화

        // 🔥 날짜 구분 메시지이면 flag 설정
        if (senderId.equals(SYSTEM_SENDER_ID) && content.matches("^\\d{4}년 \\d{2}월 \\d{2}일$")) {
            message.setDateMessage(true);
        }
        return message;
    }

    /**
     * MongoDB에 ChatMessage 문서를 저장하고 ChatRoom의 마지막 활동 시간을 업데이트합니다.
     */
    @Transactional
    public ChatMessage saveMessageEntity(ChatMessage message) {
        // 1) 메시지 저장
        ChatMessage saved = chatMessageRepository.save(message);

        // 2) 채팅방 로드
        ChatRoom chatRoom = chatRoomRepository.findById(message.getRoomId())
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        // 3) attachmentType에 따라 lastMessageContent 결정
        String atype = message.getAttachmentType();
        String lastMsg;
        if ("image".equalsIgnoreCase(atype)) {
            lastMsg = "📷 사진";
        } else if ("file".equalsIgnoreCase(atype)) {
            lastMsg = "📄 파일";
        } else {
            lastMsg = message.getContent();
        }

        // 4) 채팅방에 반영
        chatRoom.setLastMessageContent(lastMsg);
        chatRoom.updateLastActivity();
        chatRoomRepository.save(chatRoom);

        return saved;
    }

    public Optional<ChatMessage> getMessageById(String messageId) {
        return chatMessageRepository.findById(messageId);
    }

    @Transactional
    public void deleteMessage(String messageId, String requesterId) {
        ChatMessage msg = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        System.out.println("메시지 삭제 시작: ID=" + messageId + ", requesterId=" + requesterId);
        if (!msg.getSenderId().equals(requesterId)) {
            System.out.println("권한 없음: senderId=" + msg.getSenderId() + ", requesterId=" + requesterId);
            throw new AccessDeniedException("No Permission.");
        }

        String attachmentUrl = msg.getAttachmentUrl();
        System.out.println("삭제 대상 메시지 ID: " + messageId + ", attachmentUrl: " + attachmentUrl + ", attachmentType: " + msg.getAttachmentType() + ", attachmentName: " + msg.getAttachmentName());
        if (attachmentUrl != null && !attachmentUrl.isEmpty()) {
            System.out.println("파일 삭제 시도: " + attachmentUrl);
            fileStorageService.deleteFile(attachmentUrl);
            msg.setAttachmentUrl(null);
            msg.setAttachmentType(null);
            msg.setAttachmentName(null);
            System.out.println("파일 관련 필드 초기화 완료");
        } else {
            System.out.println("attachmentUrl이 없거나 비어 있음");
        }

        msg.setDeleted(true);
        msg.setContent("메시지가 삭제되었습니다!");
        System.out.println("소프트 삭제 설정: deleted=" + msg.isDeleted() + ", content=" + msg.getContent());

        ChatMessage updated = chatMessageRepository.save(msg);
        System.out.println("메시지 저장 완료: ID=" + updated.getId());

        ChatMessageRequestDto dto = ChatMessageRequestDto.of(updated);
        messagingTemplate.convertAndSend(
                "/topic/chat/" + updated.getRoomId(),
                dto
        );
        System.out.println("브로드캐스트 완료: roomId=" + updated.getRoomId());

    }

    @Transactional
    public Page<ChatMessageRequestDto> getMessagesWithUnread(Long roomId, Pageable pageable, String userId) {
        Page<ChatMessage> page = getMessages(roomId, pageable, userId);
        List<ChatMessageRequestDto> dtos = page.getContent().stream()
                .map(ChatMessageRequestDto::of)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public long getUnreadCount(Long roomId, String userId) {
        // userId(String)를 employeeId(Long)로 변환
        Long employeeId;
        try {
            employeeId = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            // userId가 숫자 형식이 아닌 경우 처리
            // 예: 사용자 이름이나 이메일을 사용하는 경우 해당 사용자의 ID를 조회하는 로직 구현
            employeeId = getUserIdByIdentifier(userId);
        }

        // MySQL에서 사용자의 채팅방 참여 정보 조회
        Optional<ChatRoomParticipant> participantOpt =
                participantRepository.findByChatRoomIdAndEmployeeId(roomId, employeeId);

        // 참여 정보가 없으면 0 반환 (사용자가 채팅방에 없으므로 읽지 않은 메시지 없음)
        if (participantOpt.isEmpty()) {
            return 0;
        }

        // 참여 정보에서 joinedAt 가져오기
        LocalDateTime joinedAt = participantOpt.get().getJoinedAt();

        // MongoDB 날짜 형식으로 변환
        Date joinedAtDate = Date.from(joinedAt.atZone(ZoneId.systemDefault()).toInstant());

        // 수정된 MongoDB 쿼리 호출
        return chatMessageRepository.countUnreadMessagesAfterJoin(roomId, userId, joinedAtDate);
    }
    /**
     * 식별자(kakaoUuid, 이메일 등)로 사용자 ID를 조회하는 보조 메서드
     * @param identifier 사용자 식별자(JWT에서 추출한 값)
     * @return 직원 ID(Long)
     */
    private Long getUserIdByIdentifier(String identifier) {

        // 2. user.userId로 검색 시도
        Optional<EmployeeEntity> employeeByUserId =
                Optional.ofNullable(employeeRepository.findByUser_UserId(identifier));
        if (employeeByUserId.isPresent()) {
            return employeeByUserId.get().getId();
        }

        // 식별자로 사용자를 찾지 못한 경우 기본값 반환
        // 실제 환경에서는 예외 처리 또는 로깅 추가 권장
        return 0L;
    }

    /**
     * 날짜가 변경된 경우 또는 채팅방의 첫 메시지인 경우 SYSTEM 타입의 날짜 메시지를 삽입
     */
    public void insertDateSeparatorIfNeeded(Long roomId) {
        // 채팅방의 가장 최근 메시지 2개 조회
        List<ChatMessage> lastTwo = chatMessageRepository.findTop2ByRoomIdOrderByTimestampDesc(roomId);

        // 채팅방에 메시지가 없거나 (첫 메시지인 경우) 또는 마지막 메시지의 날짜가 오늘과 다른 경우
        if (lastTwo.isEmpty() || lastTwo.size() < 2) {
            // 채팅방에 메시지가 없거나 하나만 있는 경우 (첫 메시지 또는 두 번째 메시지)
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            String formatted = today.format(DATE_FORMATTER);
            ChatMessage dateMsg = createSystemMessage(roomId, SYSTEM_SENDER_ID, formatted);
            chatMessageRepository.save(dateMsg);
            messagingTemplate.convertAndSend("/topic/chat/" + roomId,
                    ChatMessageRequestDto.of(dateMsg));
            return;
        }

        // 채팅방에 메시지가 2개 이상 있는 경우, 날짜 변경을 확인
        LocalDate lastDate = lastTwo.stream()
                .filter(msg -> msg.getType() == ChatMessage.MessageType.CHAT)
                .map(msg -> msg.getTimestamp().toLocalDate())
                .findFirst()
                .orElse(null);

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (lastDate == null || !lastDate.equals(today)) {
            String formatted = today.format(DATE_FORMATTER);
            ChatMessage dateMsg = createSystemMessage(roomId, SYSTEM_SENDER_ID, formatted);
            chatMessageRepository.save(dateMsg);
            messagingTemplate.convertAndSend("/topic/chat/" + roomId,
                    ChatMessageRequestDto.of(dateMsg));
        }
    }

    @Transactional
    public List<ChatMessage> markAllMessagesAsRead(Long roomId, String userId) {
        // ① userId가 null인지 검증 (컨트롤러에서 이미 검증해 주었지만, 이곳에서도 안전장치를 두어도 좋습니다)
        if (userId == null) {
            log.warn("markAllMessagesAsRead 호출 시 userId가 null입니다. roomId={}", roomId);
            return Collections.emptyList();
        }

        // ② roomId와 userId에 해당하는 “읽지 않은 메시지” 목록을 가져옴
        List<ChatMessage> unreadMessages =
                chatMessageRepository.findUnreadMessagesByRoomIdAndUserId(roomId, userId);

        // ③ 각각 readBy에 userId를 추가하고 저장
        for (ChatMessage message : unreadMessages) {
            if (!message.getReadBy().contains(userId)) {
                message.getReadBy().add(userId);
                ChatMessage savedMessage = chatMessageRepository.save(message);

                // 메시지별 읽음 상태 브로드캐스트 (optional)
                messagingTemplate.convertAndSend(
                        "/topic/chat/" + roomId + "/read",
                        Map.of(
                                "messageId", savedMessage.getId(),
                                "userId", userId
                        )
                );
                log.info("Broadcasted read status for message {} by user {} to /topic/chat/{}/read",
                        savedMessage.getId(), userId, roomId);
            }
        }

        // ④ unreadCount를 계산한 뒤, 전체 참가자 대상 브로드캐스트
        //    아래에서 payload를 만들기 위해 ChatRoom 엔티티를 다시 가져옴
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

        // 'HashMap'을 사용해 null-safe하게 payload 구성
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);

        // 전체 참가자 목록을 순회하며 각자의 unreadCount를 계산
        Map<String, Long> unreadCounts = chatRoom.getActiveParticipants().stream()
                .map(p -> p.getUser().getUserId())
                .collect(Collectors.toMap(
                        participantId -> participantId,
                        participantId -> getUnreadCount(roomId, participantId)
                ));

        payload.put("unreadCounts", unreadCounts);

        // ★ lastMessageContent가 null일 수 있으므로, null 대신 빈 문자열로 대체
        String lastMsg = chatRoom.getLastMessageContent();
        if (lastMsg == null) {
            lastMsg = "";
        }
        payload.put("lastMessageContent", lastMsg);

        // ⑤ '/topic/chat/{roomId}/unread-count'으로 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/chat/" + roomId + "/unread-count",
                payload
        );
        log.info("Broadcasted unreadCount={} for room {} and user {} to /topic/chat/{}/unread-count",
                unreadCounts, roomId, userId, roomId);

        return unreadMessages;
    }

    /**
     * 사용자의 채팅방 목록을 마지막 메시지와 읽지 않은 메시지 수와 함께 조회
     */
    public List<ChatRoomListDto> getUserChatRoomsWithLastMessage(String userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findByParticipantUserId(userId);

        return chatRooms.stream()
                .filter(room -> room.hasActiveParticipant(userId))
                .map(room -> {
                    // DB에 저장된 lastMessageContent 사용
                    String lastMessageContent = room.getLastMessageContent();

                    // lastMessageContent가 null이거나 비어있는 경우에만 마지막 메시지 조회
                    if (lastMessageContent == null || lastMessageContent.trim().isEmpty()) {
                        Optional<ChatMessage> lastMsgOpt =
                                chatMessageRepository.findTop1ByRoomIdOrderByTimestampDesc(room.getId());

                        if (lastMsgOpt.isPresent()) {
                            ChatMessage lastMsg = lastMsgOpt.get();
                            String atype = lastMsg.getAttachmentType();
                            if ("image".equalsIgnoreCase(atype)) {
                                lastMessageContent = "📷 사진";
                            } else if ("file".equalsIgnoreCase(atype)) {
                                lastMessageContent = "📄 파일";
                            } else {
                                lastMessageContent = lastMsg.getContent();
                            }
                        }
                    }

                    long unreadCount = getUnreadCount(room.getId(), userId);
                    ChatRoomListDto dto = ChatRoomListDto.of(room, userId, unreadCount);
                    dto.setLastMessageContent(lastMessageContent);
                    return dto;
                })
                .sorted((a, b) -> b.getLastActivity().compareTo(a.getLastActivity()))
                .collect(Collectors.toList());
    }

}