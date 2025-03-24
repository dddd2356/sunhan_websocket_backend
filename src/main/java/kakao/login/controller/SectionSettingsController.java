package kakao.login.controller;

import kakao.login.entity.SectionSettings;
import kakao.login.service.SectionSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/detail/department/settings")
public class SectionSettingsController {

    @Autowired
    private SectionSettingsService sectionSettingsService;

    // 1. 구역 설정 조회 API
    @GetMapping("/sections")
    public ResponseEntity<?> getSectionSettings() {
        try {
            Boolean useSections = sectionSettingsService.getUseSectionsStatus();
            Map<String, Object> response = new HashMap<>();
            response.put("useSections", useSections);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "구역 설정 조회에 실패했습니다.");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // 2. 구역 설정 변경 API
    @PutMapping("/sections")
    public ResponseEntity<?> updateSectionSettings(@RequestBody Map<String, Boolean> request) {
        try {
            Boolean useSections = request.get("useSections");
            if (useSections == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "useSections 필드가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            SectionSettings updatedSettings = sectionSettingsService.updateUseSectionsStatus(useSections);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "구역 " + (useSections ? "사용" : "사용 안함") + " 설정이 적용되었습니다.");
            response.put("useSections", updatedSettings.getUseSections());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "구역 설정 변경에 실패했습니다.");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}