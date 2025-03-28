//package kakao.login.controller;
//
//
//import jakarta.servlet.http.HttpServletRequest;
//import kakao.login.dto.request.message.MessageRequestDto;
//import kakao.login.service.KakaoMessageService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/v1/detail/messages")
//@RequiredArgsConstructor
//public class KakaoMessageController {
//
//    private final KakaoMessageService kakaoMessageService;
//
//    @PostMapping("/send")
//    public ResponseEntity<?> sendMessage(@RequestBody MessageRequestDto request, HttpServletRequest requestHttp) {
//
//        // 헤더에서 accessToken 가져오기
//        String header = requestHttp.getHeader("Authorization");
//        if (header == null || header.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access token is missing or invalid.");
//        }
//
//        // "Bearer " 접두사가 있다면 제거
//        String accessToken = header.startsWith("Bearer ") ? header.substring(7) : header;
//
//        // 메시지 전송
//        boolean result = kakaoMessageService.sendMessage(request, accessToken);  // accessToken 전달
//        if (result) {
//            return ResponseEntity.ok("메시지 전송 성공");
//        } else {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("메시지 전송 실패");
//        }
//    }
//
//
//
//}