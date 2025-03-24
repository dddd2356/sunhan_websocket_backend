package kakao.login.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kakao.login.dto.request.message.MessageRequestDto;
import kakao.login.entity.EmployeeEntity;
import kakao.login.entity.UserEntity;
import kakao.login.repository.EmployeeRepository;
import kakao.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KakaoMessageService {

    @Value("${kakao.admin-key}")
    private String kakaoAdminKey;

    @Value("${kakao.template-id}")
    private String templateId;

    private final WebClient webClient;
    private final EmployeeService employeeService;
    private final UserRepository userRepository;  // UserRepository 추가
    private final EmployeeRepository employeeRepository;  // EmployeeRepository 추가

    public boolean sendMessage(MessageRequestDto request, String accessToken) {
        // 📌 전송 타입에 따른 UUID 목록 설정
        List<String> targetKakaoUuids;
        switch (request.getSendType()) {
            case ALL:
                targetKakaoUuids = employeeService.getAllKakaoUuids();
                break;
            case DEPARTMENT:
                System.out.println("전송 요청된 부서명 목록: " + request.getDepartmentIds());

                if (request.getDepartmentIds() == null || request.getDepartmentIds().isEmpty()) {
                    System.out.println("⚠️ 선택된 부서가 없습니다.");
                    return false;
                }

                targetKakaoUuids = request.getDepartmentIds().stream()
                        .flatMap(deptName -> {
                            List<String> deptUuids = employeeService.getKakaoUuidsByDepartment(deptName);
                            System.out.println("부서 [" + deptName + "]의 UUID 목록: " + deptUuids);
                            return deptUuids.stream();
                        })
                        .collect(Collectors.toList());

                System.out.println("최종 대상 UUID 목록: " + targetKakaoUuids);

                if (targetKakaoUuids.isEmpty()) {
                    System.out.println("⚠️ 선택된 부서에 유효한 UUID를 가진 직원이 없습니다.");
                    return false;
                }
                break;
            case INDIVIDUAL:
                targetKakaoUuids = employeeService.getKakaoUuidsWithEmployees(request.getEmployeeIds());
                break;
            default:
                throw new IllegalArgumentException("잘못된 전송 타입");
        }

        // 📌 카카오 친구 목록 조회
        List<Map<String, Object>> kakaoFriends = getKakaoFriends(accessToken);

        // 📌 메시지를 보낼 수 있는 친구 필터링
        List<String> receiverUuids = kakaoFriends.stream()
                .filter(friend -> "true".equals(friend.get("allowed_msg").toString())) // "true"로 비교
                .map(friend -> (String) friend.get("uuid"))
                .filter(targetKakaoUuids::contains) // 직원의 UUID와 일치하는 친구만
                .collect(Collectors.toList());

        if (receiverUuids.isEmpty()) {
            // 디버깅을 위한 출력
            System.out.println("🔴 메시지를 보낼 수 있는 친구가 없습니다. 친구 목록: " + kakaoFriends);
            System.out.println("🔴 메시지를 보낼 수 있는 직원 UUID 목록: " + targetKakaoUuids);
            return false;  // 예외를 던지지 않고 false 반환
        }

        // 📌 카카오 메시지 전송 API 호출
        String url = "https://kapi.kakao.com/v1/api/talk/friends/message/send";

        // 📌 JSON 배열로 변환
        String receiverUuidsJson = convertToJson(receiverUuids);

        // 📌 폼 데이터 생성
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("template_id", templateId);
        formData.add("receiver_uuids", receiverUuidsJson);

        // 메시지가 존재할 경우 template_args에 포함
        if (request.getMessage() != null && !request.getMessage().isEmpty()) {
            formData.add("template_args", "{\"message\":\"" + request.getMessage() + "\"}");
        }

        System.out.println("전송 파라미터: " + formData);
        System.out.println("대상 사용자 UUID 목록: " + receiverUuids);


        try {
            Map<String, Object> response = webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formData)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            System.out.println("API 응답: " + response);

            if (response != null) {
                // 성공한 사용자 UUID 리스트 가져오기
                List<String> successfulReceivers = (List<String>) response.get("successful_receiver_uuids");

                if (successfulReceivers != null && !successfulReceivers.isEmpty()) {
                    System.out.println("✅ 전송 성공! 성공한 UUID 리스트: " + successfulReceivers);
                    return true;
                } else {
                    System.out.println("❌ 메시지 전송 실패: 성공한 UUID가 없음");

                    // 실패 정보 확인
                    List<Map<String, Object>> failureInfo = (List<Map<String, Object>>) response.get("failure_info");
                    if (failureInfo != null && !failureInfo.isEmpty()) {
                        for (Map<String, Object> failure : failureInfo) {
                            System.out.println("실패 코드: " + failure.get("code"));
                            System.out.println("실패 메시지: " + failure.get("msg"));
                            System.out.println("실패한 UUID 리스트: " + failure.get("receiver_uuids"));
                        }
                    }
                    return false;
                }
            } else {
                System.out.println("❌ 카카오톡 메시지 전송 실패: 응답이 없음");
                return false;
            }
        } catch (Exception e) {
            System.err.println("카카오톡 메시지 전송 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

        // 📌 카카오 친구 목록 조회
    public List<Map<String, Object>> getKakaoFriends(String accessToken) {
        String url = "https://kapi.kakao.com/v1/api/talk/friends";
        try {
            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("elements")) {
                System.out.println("❌ 친구 목록이 없습니다.");
                return Collections.emptyList();
            }

            List<Map<String, Object>> friends = (List<Map<String, Object>>) response.get("elements");
            System.out.println("✅ 카카오 친구 목록 응답: " + friends);

            // 카카오 친구 목록 업데이트 후 UUID 업데이트
            for (Map<String, Object> friend : friends) {
                String friendUuid = (String) friend.get("uuid");  // 카카오 API에서 받은 uuid
                Object idObj = friend.get("id");  // "id" 필드 사용

                if (friendUuid != null && idObj != null) {
                    String rawUserId = idObj.toString();  // 숫자 id를 문자열로 변환
                    String userId = "kakao_" + rawUserId;  // "kakao_" 접두어 추가

                    // 유저가 존재하는지 확인
                    UserEntity userEntity = userRepository.findByUserId(userId);
                    if (userEntity == null) {
                        System.out.println("❌ 사용자 없음: " + userId);
                        // 만약 사용자 등록이 필수라면, 여기서 신규 등록 처리 혹은 로그를 남깁니다.
                    } else {
                        // 유저가 존재하면 카카오 UUID 업데이트
                        if (userEntity.getKakaoUuid() == null || userEntity.getKakaoUuid().isEmpty()) {
                            // 카카오 UUID가 없으면 업데이트 (업데이트 쿼리도 userId 기준으로 수정)
                            userRepository.updateKakaoUuid(userId, friendUuid);
                            System.out.println("✅ 카카오 UUID 업데이트 완료: " + userId);
                            // EmployeeEntity 업데이트
                            employeeRepository.updateEmployeeKakaoUuid(userId, friendUuid);
                            System.out.println("✅ 직원 테이블에 카카오 UUID 업데이트 완료: " + userId);
                        } else {
                            System.out.println("❌ 해당 사용자에 대한 유효한 카카오 UUID가 이미 존재: " + userId);
                        }
                    }
                }
            }
            return friends;
        } catch (Exception e) {
            System.out.println("❌ 카카오 친구 목록 API 호출 실패: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // 📌 리스트를 JSON 배열 문자열로 변환
    private String convertToJson(List<String> list) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            System.err.println("JSON 변환 오류: " + e.getMessage());
            return "[]"; // 빈 배열 반환
        }
    }
}