package kakao.login.controller;

import kakao.login.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/employees") // 프론트엔드가 요청하는 경로
@RequiredArgsConstructor
public class EmployeeProfileController {

    private final EmployeeService employeeService;

    @GetMapping(value = "/{employeeId}/profile-image", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    @PreAuthorize("isAuthenticated()") // "로그인한 사용자"는 누구나 접근 가능하도록 설정
    public ResponseEntity<byte[]> getEmployeeProfileImage(@PathVariable Long employeeId) {
        byte[] image = employeeService.getProfileImage(employeeId);

        if (image == null || image.length == 0) {
            // 이미지가 없으면 404 Not Found 응답
            return ResponseEntity.notFound().build();
        }

        // 브라우저가 24시간 동안 이미지를 캐시하도록 설정 (성능 향상)
        CacheControl cacheControl = CacheControl.maxAge(24, TimeUnit.HOURS).noTransform().mustRevalidate();

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .body(image);
    }
}