package kakao.login.service.implement;

import com.fasterxml.jackson.databind.ObjectMapper;
import kakao.login.entity.CustomOAuth2User;
import kakao.login.entity.UserEntity;
import kakao.login.repository.UserRepository;
import kakao.login.service.KakaoMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuth2UserServiceImplement extends DefaultOAuth2UserService {

    // 필요한 의존성 주입
    private final UserRepository userRepository;
    private final KakaoMessageService kakaoMessageService; // KakaoMessageService를 사용하여 친구 목록을 가져옵니다.

    // OAuth2 인증 정보를 처리하는 메서드
    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {

        // 부모 클래스의 loadUser()를 호출하여 OAuth2User 객체를 가져옵니다.
        OAuth2User oAuth2User = super.loadUser(request);

        // 클라이언트 이름을 가져옵니다. (kakao 또는 naver)
        String oauthClientName = request.getClientRegistration().getClientName();

        // 로그를 출력하여 인증된 사용자 정보를 확인합니다.
        try {
            System.out.println(new ObjectMapper().writeValueAsString(oAuth2User.getAttributes()));
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        // 사용자 정보를 담을 변수들 선언
        UserEntity userEntity = null;
        String userId = null;
        String email = null;
        String kakaoUuid = null;

        // 카카오 로그인인 경우
        if (oauthClientName.equals("kakao")) {
            // 카카오 로그인 후 받은 id는 userId로 사용
            userId = "kakao_" + oAuth2User.getAttributes().get("id").toString();  // Long을 String으로 변환

            // 카카오 계정 정보에서 이메일을 가져옵니다.
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
            if (kakaoAccount != null && kakaoAccount.containsKey("email")) {
                email = (String) kakaoAccount.get("email");
            }

            // 카카오 UUID는 친구 목록에서 받아오는 값이어야 함
            // KakaoMessageService를 사용하여 카카오 친구 목록을 가져오고, 해당 UUID를 찾아서 설정
            String accessToken = request.getAccessToken().getTokenValue(); // 액세스 토큰
            kakaoUuid = kakaoMessageService.getKakaoFriends(accessToken).stream()
                    .filter(friend -> friend.get("allowed_msg").equals("true")) // 메시지 허용 친구 필터링
                    .map(friend -> (String) friend.get("uuid"))
                    .findFirst()
                    .orElse(null); // 첫 번째 유효한 카카오 UUID 가져오기
        } else if (oauthClientName.equals("naver")) {
            // 네이버 로그인인 경우
            Map<String, String> responseMap = (Map<String, String>) oAuth2User.getAttributes().get("response");
            userId = "naver_" + responseMap.get("id").substring(0, 14);  // 네이버 ID는 14자리만 사용
            email = responseMap.get("email");
        }

        // 기존 유저 조회
        Optional<UserEntity> existingUser = userRepository.findById(userId);

        if (existingUser.isPresent()) {
            userEntity = existingUser.get();

            // 이메일이 변경되었을 경우 업데이트
            if (email != null && !email.equals(userEntity.getEmail())) {
                userEntity.setEmail(email);
                userRepository.save(userEntity); // 이메일 업데이트
            }

        } else {
            // 새로운 사용자 생성
            userEntity = new UserEntity();
            userEntity.setUserId(userId);  // 로그인된 사용자 ID 저장
            userEntity.setPassword("Passw0rd");  // 기본 패스워드
            userEntity.setEmail(email);  // 이메일 저장
            userEntity.setType(oauthClientName);  // OAuth 로그인 방식 (kakao, naver)
            userEntity.setRole("ROLE_USER");  // 기본 역할은 USER
            userRepository.save(userEntity);  // 사용자 정보 저장
        }

        // 카카오 UUID 값은 친구 목록에서 받아온 UUID로 처리
        if (kakaoUuid != null && (userEntity.getKakaoUuid() == null || userEntity.getKakaoUuid().isEmpty())) {
            userEntity.setKakaoUuid(kakaoUuid);  // 카카오 UUID 저장
            userRepository.save(userEntity);  // DB에 업데이트
        }

        // CustomOAuth2User로 반환 (userId만 담아줌)
        return new CustomOAuth2User(userId);
    }
}
