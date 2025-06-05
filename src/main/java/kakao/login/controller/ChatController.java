package kakao.login.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import kakao.login.dto.request.message.BulkMessageRequestDto;
import kakao.login.dto.request.message.ChatMessageRequestDto;
import kakao.login.dto.request.message.MessageRequestDto;
import kakao.login.dto.request.room.ChatRoomRequestDto;
import kakao.login.entity.ChatMessage;
import kakao.login.entity.ChatRoom;
import kakao.login.entity.EmployeeEntity;
import kakao.login.repository.EmployeeRepository;
import kakao.login.service.ChatMessageService;
import kakao.login.service.ChatRoomService;
import kakao.login.service.EmployeeService;
import kakao.login.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Tag(name = "Chat API", description = "채팅방·메시지 관련 기능")
@RestController
@RequestMapping("/api/v1/chat")
@Slf4j
public class ChatController {

    private final ChatMessageService chatService;
    private final EmployeeService employeeService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomService chatRoomService;
    private final EmployeeRepository employeeRepository;

    private final FileStorageService fileStorageService;

    @Autowired
    public ChatController(ChatMessageService chatMessageService,
                          EmployeeService employeeService,
                          SimpMessagingTemplate messagingTemplate,
                          ChatRoomService chatRoomService,
                          EmployeeRepository employeeRepository,
                          FileStorageService fileStorageService) {
        this.chatService = chatMessageService;
        this.employeeService = employeeService;
        this.messagingTemplate = messagingTemplate;
        this.chatRoomService = chatRoomService;
        this.employeeRepository = employeeRepository;
        this.fileStorageService = fileStorageService;
    }

    @Operation(
            summary = "채팅방 생성",
            description = "그룹/1:1 채팅방을 생성합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "채팅방 생성 성공"),
                    @ApiResponse(responseCode = "400", description = "요청 형식 오류")
            }
    )
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomRequestDto> createChatRoom(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal String loginUserId) {
        String name = (String) payload.get("name");
        String creatorId = (String) payload.get("creatorId");
        boolean isGroupChat = Boolean.parseBoolean(payload.get("isGroupChat").toString());

        ChatRoom room = chatRoomService.createChatRoom(name, creatorId, isGroupChat);
        ChatRoomRequestDto dto = chatRoomService.toDto(room, loginUserId);
        return ResponseEntity.ok(dto);
    }

    @Operation(
            summary = "채팅방 조회",
            description = "채팅방 ID로 단일 채팅방을 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "채팅방 조회 성공"),
                    @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
            }
    )
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomRequestDto> getChatRoomById(
            @PathVariable Long roomId,
            @AuthenticationPrincipal String loginUserId) {
        return chatRoomService.getChatRoom(roomId)
                .map(room -> ResponseEntity.ok(chatRoomService.toDto(room, loginUserId)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rooms/{roomId}/participants")
    public ResponseEntity<List<EmployeeEntity>> getParticipants(@PathVariable Long roomId) {
        List<EmployeeEntity> participants = chatService.getParticipants(roomId);
        return ResponseEntity.ok(participants);
    }

    @PostMapping("/rooms/{roomId}/participants")
    public ResponseEntity<ChatRoomRequestDto> addParticipant(
            @PathVariable Long roomId,
            @RequestParam String userId,
            @AuthenticationPrincipal String loginUserId) {
        ChatRoom room = chatService.addParticipant(roomId, userId);
        messagingTemplate.convertAndSend(
                "/topic/chat/" + roomId + "/participants",
                room.getActiveParticipants()
        );
        ChatRoomRequestDto dto = chatRoomService.toDto(room, loginUserId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/rooms/{roomId}/invite")
    public ResponseEntity<ChatRoomRequestDto> inviteParticipant(
            @PathVariable Long roomId,
            @RequestBody Map<String, String> request,           // "employeeId", "content" 포함
            @AuthenticationPrincipal String loginUserId) {
        String inviterId = loginUserId;
        Long employeeId = Long.valueOf(request.get("employeeId"));
        String content = request.get("content");            // 추가된 실제 메시지 내용

        ChatRoom chatRoom = chatRoomService.getChatRoom(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));
        if (!chatRoom.hasActiveParticipant(inviterId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 1) 초대 시스템 메시지
        EmployeeEntity inviter = employeeRepository.findByUser_UserId(inviterId);
        EmployeeEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        String inviteMsg = inviter.getName() + "님이 " + employee.getName() + "님을 초대했습니다.";
        ChatMessage sysMsg = chatService.createSystemMessage(roomId, inviterId, inviteMsg);
        sysMsg.setInviteMessage(true);
        chatService.sendMessage(sysMsg);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, sysMsg);

        // 2) 사용자 실제 메시지
        if (content != null && !content.isBlank()) {
            ChatMessage userMsg = chatService.createMessageFromRequest(roomId, inviterId, content);
            chatService.sendMessage(userMsg);
            //messagingTemplate.convertAndSend("/topic/chat/" + roomId, userMsg);
        }

        // 3) 방에 사용자 추가 및 참가자 목록 브로드캐스트
        ChatRoom updatedRoom = chatRoomService.addUserToChatRoom(roomId, employee.getUser().getUserId());
        messagingTemplate.convertAndSend(
                "/topic/chat/" + roomId + "/participants",
                updatedRoom.getActiveParticipants()
        );
        ChatRoomRequestDto dto = chatRoomService.toDto(updatedRoom, loginUserId);
        return ResponseEntity.ok(dto);
    }



    @DeleteMapping("/rooms/{roomId}/participants/{userId}")
    public ResponseEntity<ChatRoomRequestDto> removeParticipant(
            @PathVariable Long roomId,
            @PathVariable String userId,
            @AuthenticationPrincipal String loginUserId) {
        ChatRoom room = chatService.removeParticipant(roomId, userId);
        messagingTemplate.convertAndSend(
                "/topic/chat/" + roomId + "/participants",
                room.getActiveParticipants()
        );
        ChatRoomRequestDto dto = chatRoomService.toDto(room, loginUserId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/rooms/{roomId}/messages/read")
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable String roomId,
            @RequestParam String messageId,
            @RequestParam String userId) {
        chatService.markMessagesAsRead(messageId, userId);
        // 방의 모든 참가자에 대해 unreadCount 갱신
        ChatRoom room = chatRoomService.getChatRoom(Long.valueOf(roomId))
                .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));
        Map<String, Long> unreadCounts = new HashMap<>();
        for (String participantId : room.getActiveParticipants().stream()
                .map(p -> p.getUser().getUserId())
                .collect(Collectors.toList())) {
            long unreadCount = chatService.getUnreadCount(Long.valueOf(roomId), participantId);
            unreadCounts.put(participantId, unreadCount);
        }
        messagingTemplate.convertAndSend(
                "/topic/chat/" + roomId + "/unread-count",
                Map.of(
                        "userId", userId,
                        "unreadCounts", unreadCounts
                )
        );
        log.info("Broadcasted unreadCounts for room {} after single message read: {}", roomId, unreadCounts);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/direct")
    public ResponseEntity<?> createDirectChatRoom(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal String loginUserId) {
        String employee1Id = request.get("employee1Id");
        String employee2Id = request.get("employee2Id");
        if (employee1Id == null || employee2Id == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "INVALID_REQUEST", "message", "employee1Id and employee2Id are required"));
        }
        try {
            Long emp1Id = Long.parseLong(employee1Id);
            Long emp2Id = Long.parseLong(employee2Id);
            // Check if the authenticated user is one of the participants
            EmployeeEntity emp1 = employeeRepository.findById(emp1Id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee with ID " + emp1Id + " not found"));
            EmployeeEntity emp2 = employeeRepository.findById(emp2Id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee with ID " + emp2Id + " not found"));
            if (!emp1.getUser().getUserId().equals(loginUserId) && !emp2.getUser().getUserId().equals(loginUserId)) {
                log.warn("User {} attempted to create chat room for employee IDs {} and {} without permission", loginUserId, emp1Id, emp2Id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("code", "NP", "message", "No Permission."));
            }
            ChatRoom room = chatRoomService.getOrCreateDirectChatRoomByEmployeeId(emp1Id, emp2Id);
            ChatRoomRequestDto dto = chatRoomService.toDto(room, loginUserId);
            return ResponseEntity.ok(dto);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "INVALID_REQUEST", "message", "Invalid employee ID format"));
        }
    }


    @PostMapping("/group")
    public ResponseEntity<ChatRoomRequestDto> createGroupChatRoom(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal String loginUserId) {
        String name = (String) request.get("name");
        @SuppressWarnings("unchecked")
        List<String> participantIds = (List<String>) request.get("participantIds");

        ChatRoom room = chatRoomService.createGroupChatRoom(name, participantIds, loginUserId);
        ChatRoomRequestDto dto = chatRoomService.toDto(room, loginUserId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/rooms/user/{userId}")
    public ResponseEntity<List<ChatRoomRequestDto>> getUserChatRooms(
            @PathVariable String userId,
            @AuthenticationPrincipal String loginUserId) {
        List<ChatRoom> rooms = chatRoomService.getUserChatRooms(userId);
        List<ChatRoomRequestDto> dtos = rooms.stream()
                .map(room -> chatRoomService.toDto(room, loginUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }


    @GetMapping("/rooms/department")
    public ResponseEntity<List<ChatRoomRequestDto>> getChatRoomsByDepartment(
            @RequestParam String departmentName,
            @AuthenticationPrincipal String loginUserId) {
        List<ChatRoom> chatRooms = chatService.getChatRoomsByDepartment(departmentName);
        List<ChatRoomRequestDto> dtos = chatRooms.stream()
                .map(room -> chatRoomService.toDto(room, loginUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/rooms/department/section")
    public ResponseEntity<List<ChatRoomRequestDto>> getChatRoomsByDepartmentAndSection(
            @RequestParam String departmentName,
            @RequestParam String sectionName,
            @AuthenticationPrincipal String loginUserId) {
        List<ChatRoom> chatRooms = chatService.getChatRoomsByDepartmentAndSection(departmentName, sectionName);
        List<ChatRoomRequestDto> dtos = chatRooms.stream()
                .map(room -> chatRoomService.toDto(room, loginUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Hidden
    @MessageMapping("/chat.sendMessage")
    @Transactional
    public void handleChatMessage(@Payload Map<String, Object> messageMap) {
        String senderId = (String) messageMap.get("senderId");
        String content = (String) messageMap.get("content");
        Long roomId = Long.valueOf(messageMap.get("roomId").toString());

        ChatRoom chatRoom = chatService.getChatRoomById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        ChatMessage savedMessage;
        if (!chatRoom.isGroupChat()) {
            savedMessage = chatService.sendDirectMessage(roomId, senderId, content, false);
        } else {
            ChatMessage chatMessage = chatService.createMessageFromRequest(roomId, senderId, content);
            savedMessage = chatService.sendMessage(chatMessage);
        }
        ChatMessageRequestDto dto = ChatMessageRequestDto.of(savedMessage);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, dto);
    }

    @PostMapping("/direct/message")
    public ResponseEntity<?> sendDirectMessage(@RequestBody Map<String, Object> messageRequest) {
        String senderId = (String) messageRequest.get("senderId");
        String content = (String) messageRequest.get("content");
        Long roomId = Long.valueOf(messageRequest.get("roomId").toString());
        boolean invite = Boolean.parseBoolean(messageRequest.getOrDefault("invite", "false").toString());

        ChatMessage message = chatService.sendDirectMessage(roomId, senderId, content, invite);
        if (!invite && message != null) {
            messagingTemplate.convertAndSend("/topic/chat/" + roomId, message);
            return ResponseEntity.ok(message);
        } else {
            return ResponseEntity.ok().build();
        }
    }

    @PostMapping("/rooms/{roomId}/exit")
    public ResponseEntity<?> exitChatRoom(@PathVariable Long roomId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth.getName();
            log.info("User {} attempting to exit room {}", userId, roomId);

            EmployeeEntity employee = employeeRepository.findByUser_UserId(userId);
            if (employee == null) {
                log.error("User not found: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            ChatRoom chatRoom = chatRoomService.getChatRoom(roomId)
                    .orElse(null);
            if (chatRoom == null) {
                log.error("Chat room not found: {}", roomId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Chat room not found");
            }

            if (!chatRoom.hasActiveParticipant(userId)) {
                log.error("User {} is not a participant in room {}", userId, roomId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is not a participant of this chat room");
            }

            // 1:1 채팅방일 경우 시스템 메시지 생략
            if (chatRoom.isGroupChat()) {
                String exitMessage = employee.getName() + "님이 채팅방을 나갔습니다.";
                ChatMessage exitNotification = chatService.createSystemMessage(roomId, userId, exitMessage);
                exitNotification.setExitMessage(true);
                // DB 저장
                chatService.saveMessageEntity(exitNotification);
                // 브로드캐스트
                messagingTemplate.convertAndSend("/topic/chat/" + roomId, ChatMessageRequestDto.of(exitNotification));
            }

            // 참가자 제거 및 목록 브로드캐스트
            ChatRoom updatedRoom = chatRoomService.removeUserFromChatRoom(roomId, userId);
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + roomId + "/participants",
                    updatedRoom.getActiveParticipants()
            );

            log.info("User {} successfully exited room {}", userId, roomId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error exiting chat room", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to exit chat room: " + e.getMessage());
        }
    }

    /**
     * 파일/이미지 업로드 후, 해당 첨부 메시지를 생성·브로드캐스트
     */
    @PostMapping("/rooms/{roomId}/attachments")
    public ResponseEntity<ChatMessageRequestDto> uploadAttachment(
            @PathVariable Long roomId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String senderId = auth.getName();
        EmployeeEntity sender = employeeRepository.findByUser_UserId(senderId);
        if (sender == null) {
            throw new RuntimeException("Sender not found with userId: " + senderId);
        }

        chatService.insertDateSeparatorIfNeeded(roomId);

        ChatRoom room = chatRoomService.getChatRoom(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));
        Hibernate.initialize(room.getChatRoomParticipants());
        int activeCount = (int) room.getActiveParticipants().stream()
                .filter(p -> !p.getUser().getUserId().equals(senderId))
                .count();

        String contentType = file.getContentType();
        boolean isImage = contentType != null && contentType.startsWith("image/");
        ChatMessage uploadingMsg = new ChatMessage();
        uploadingMsg.setRoomId(roomId);
        uploadingMsg.setSenderId(senderId);
        uploadingMsg.setSenderName(sender.getName());
        uploadingMsg.setTimestamp(LocalDateTime.now());
        uploadingMsg.setType(ChatMessage.MessageType.CHAT);
        uploadingMsg.setContent("");
        uploadingMsg.setAttachmentType(isImage ? "image" : "file");
        uploadingMsg.setAttachmentName(file.getOriginalFilename());
        uploadingMsg.setStatus("uploading");
        uploadingMsg.setParticipantCountAtSend(activeCount);
        uploadingMsg.setReadBy(new ArrayList<>(List.of(senderId)));
        ChatMessageRequestDto uploadingDto = ChatMessageRequestDto.of(uploadingMsg);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, uploadingDto);

        String relativePath = fileStorageService.store(file);
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String absoluteUrl = baseUrl + relativePath;

        boolean fileReady = false;
        for (int i = 0; i < 10; i++) {
            try {
                java.net.URL url = new java.net.URL(absoluteUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(500);
                conn.setReadTimeout(500);
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    fileReady = true;
                    break;
                }
            } catch (Exception e) {
                log.warn("파일 접근 시도 실패 (시도 {}/10): {}", i + 1, e.getMessage());
            }
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        if (!fileReady) {
            log.error("파일이 5초 내에 접근 불가: {}", absoluteUrl);
            throw new RuntimeException("파일 업로드 후 접근 실패: " + absoluteUrl);
        }

        ChatMessage msg = new ChatMessage();
        msg.setRoomId(roomId);
        msg.setSenderId(senderId);
        msg.setSenderName(sender.getName());
        msg.setTimestamp(LocalDateTime.now());
        msg.setType(ChatMessage.MessageType.CHAT);
        msg.setContent("");
        msg.setAttachmentType(isImage ? "image" : "file");
        msg.setAttachmentUrl(absoluteUrl);
        msg.setAttachmentName(file.getOriginalFilename());
        msg.setParticipantCountAtSend(activeCount);
        msg.setReadBy(new ArrayList<>(List.of(senderId)));
        msg.setStatus(null);

        ChatMessage saved = chatService.saveMessageEntity(msg);

        ChatMessageRequestDto dto = ChatMessageRequestDto.of(saved);
        log.info("브로드캐스트 메시지: {}", dto); // 디버깅 로그 강화
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, dto);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/attachments/download/{filename:.+}")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable String filename,
            HttpServletRequest request) throws IOException {

        // 1) 실제 파일 리소스 로드
        Resource resource = fileStorageService.loadAsResource(filename);
        log.info("File path: {}", resource.getFile().getAbsolutePath());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        // 2) 브라우저가 알맞게 처리할 수 있도록 Content-Type 유추
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // 파일 타입을 결정할 수 없는 경우
        }

        // Content-Type이 null이면 기본값 설정
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // 3) attachment 헤더 달아서 리턴
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000") // 1년 캐싱
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/rooms/{roomId}/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable Long roomId,
            @PathVariable String messageId) {

        String userId = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        try {
            chatService.deleteMessage(messageId, userId);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (AccessDeniedException ex) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of("code","NP","message","No Permission."));
        }
    }


    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<ChatMessageRequestDto>> getMessages(
            @PathVariable Long roomId,
            Pageable pageable,
            @RequestParam String userId) {
        Page<ChatMessageRequestDto> dtos = chatService.getMessagesWithUnread(roomId, pageable, userId);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/rooms/{roomId}/unread-count")
    public ResponseEntity<Map<String, Object>> getMyUnreadCount(
            @PathVariable Long roomId,
            @AuthenticationPrincipal String userId  // JWT Principal에서 userId 조회
    ) {
        long count = chatService.getUnreadCount(roomId, userId);
        return ResponseEntity.ok(Map.of(
                "roomId", roomId,
                "unreadCount", count
        ));
    }
    
    @PostMapping("/broadcast/all")
    public ResponseEntity<?> broadcastToAll(
            @RequestBody MessageRequestDto request,
            @AuthenticationPrincipal String loginUserId) {

        String content = request.getMessage();
        List<EmployeeEntity> all = employeeService.getAllEmployees();

        for (EmployeeEntity e : all) {
            String targetUserId = e.getUser().getUserId();
            if (targetUserId.equals(loginUserId)) continue;

            // 1:1 대화방 생성 또는 조회
            ChatRoom room = chatRoomService.getOrCreateDirectChatRoom(loginUserId, targetUserId);

            // 메시지 저장
            ChatMessage saved = chatService.sendDirectMessage(
                    room.getId(), loginUserId, content, false);

            // DTO로 변환
            ChatMessageRequestDto dto = ChatMessageRequestDto.of(saved);

            // WebSocket 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + room.getId(),
                    dto
            );
        }

        return ResponseEntity.ok(Map.of("status", "broadcasted to all"));
    }


    /**
     * 2) 특정 부서 직원에게만 메시지 보내기
     */
    @PostMapping("/broadcast/department")
    public ResponseEntity<?> broadcastToDepartment(
            @RequestBody MessageRequestDto request,
            @AuthenticationPrincipal String loginUserId) {

        // 1) departmentIds 반드시 체크
        List<String> deptIds = request.getDepartmentIds();
        if (deptIds == null || deptIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "departmentIds must be provided for DEPARTMENT sendType"));
        }

        // 2) 로그인 유저 조회
        EmployeeEntity me = employeeRepository.findByUser_UserId(loginUserId);
        if (me == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인 정보 없음"));
        }
        String myUserId = me.getUser().getUserId();

        String content = request.getMessage();
        Map<String, String> results = new LinkedHashMap<>();

        for (String deptIdOrName : request.getDepartmentIds()) {
            List<EmployeeEntity> list = employeeService.getEmployeesByDepartment(deptIdOrName);
            for (EmployeeEntity e : list) {
                String targetUserId = e.getUser().getUserId();
                if (targetUserId.equals(myUserId)) continue;

                ChatRoom room = chatRoomService.getOrCreateDirectChatRoom(myUserId, targetUserId);
                ChatMessage saved = chatService.sendDirectMessage(
                        room.getId(), myUserId, content, false
                );

                // 1) 메시지를 DTO 로 변환
                ChatMessageRequestDto dto = ChatMessageRequestDto.of(saved);

                // 2) WebSocket 브로드캐스트
                messagingTemplate.convertAndSend(
                        "/topic/chat/" + room.getId(),
                        dto
                );

                results.put(targetUserId, saved.getId());
            }
        }

        return ResponseEntity.ok(Map.of(
                "status", "broadcasted to departments",
                "results", results
        ));
    }



    /**
     * 3) 특정 개인에게 메시지 보내기 (직원 아이디로)
     */
    @PostMapping("/broadcast/users")
    public ResponseEntity<?> sendToMultipleUsers(
            @RequestBody BulkMessageRequestDto req,
            @AuthenticationPrincipal String loginUserId) {

        EmployeeEntity me = employeeRepository.findByUser_UserId(loginUserId);
        if (me == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 정보 없음");
        }
        String myUserId = me.getUser().getUserId();

        Map<Long, String> results = new HashMap<>();
        for (Long empId : req.getEmployeeIds()) {
            try {
                EmployeeEntity target = employeeRepository.findById(empId)
                        .orElseThrow(() -> new RuntimeException("직원 없음: " + empId));
                String targetUserId = target.getUser().getUserId();

                ChatRoom room = chatRoomService.getOrCreateDirectChatRoom(myUserId, targetUserId);
                ChatMessage saved = chatService.sendDirectMessage(
                        room.getId(), myUserId, req.getMessage(), false
                );
                results.put(empId, "sent (msgId=" + saved.getId() + ")");
            } catch (Exception ex) {
                results.put(empId, "error: " + ex.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of("status","completed", "results", results));
    }

    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markRoomAsRead(
            @PathVariable Long roomId,
            @RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        List<ChatMessage> updatedMessages = chatService.markAllMessagesAsRead(roomId, userId);

        // 방의 모든 참가자에 대해 unreadCount 갱신
        ChatRoom room = chatRoomService.getChatRoom(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));
        Map<String, Long> unreadCounts = new HashMap<>();
        for (String participantId : room.getActiveParticipants().stream()
                .map(p -> p.getUser().getUserId())
                .collect(Collectors.toList())) {
            long unreadCount = chatService.getUnreadCount(roomId, participantId);
            unreadCounts.put(participantId, unreadCount);
        }

        // 단일 브로드캐스트로 모든 참가자의 unreadCount 전송
        messagingTemplate.convertAndSend(
                "/topic/chat/" + roomId + "/unread-count",
                Map.of(
                        "userId", userId,
                        "unreadCounts", unreadCounts
                )
        );
        log.info("Broadcasted unreadCounts for room {}: {}", roomId, unreadCounts);

        return ResponseEntity.ok().build();
    }

}