package kakao.login.service.implement;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kakao.login.common.CertificationNumber;
import kakao.login.dto.request.auth.*;
import kakao.login.dto.response.ResponseDto;
import kakao.login.dto.response.auth.*;
import kakao.login.entity.CertificationEntity;
import kakao.login.entity.UserEntity;
import kakao.login.provider.EmailProvider;
import kakao.login.provider.JwtProvider;
import kakao.login.repository.CertificationRepository;
import kakao.login.repository.UserRepository;
import kakao.login.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImplement implements AuthService {

    //final로 제어역전통해서 의존성주입
    private final UserRepository userRepository;
    private final CertificationRepository certificationRepository;

    private final JwtProvider jwtProvider;
    private final EmailProvider emailProvider;

    
    //로그아웃을 위해 설정
    private final RestTemplate restTemplate = new RestTemplate();

    //암호화를 위해 추가
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public ResponseEntity<? super IdCheckResponseDto> idCheck(IdCheckRequestDto dto) {
        try{

            String userId = dto.getId();
            boolean isExistId = userRepository.existsByUserId(userId);
            if(isExistId) return IdCheckResponseDto.duplicateId();

        }catch (Exception exception){
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }

        return IdCheckResponseDto.success();

    }

    @Override
    public ResponseEntity<? super EmailCertificationResponseDto> emailCertification(EmailCertificationRequestDto dto) {
        try{

            String userId = dto.getId();
            String email = dto.getEmail();

            //존재하지 않는 이메일인지 확인
            boolean isExistId = userRepository.existsByUserId(userId);
            if(isExistId) return EmailCertificationResponseDto.duplicateId();

            //certificationNumber 생성
            String certificationNumber = CertificationNumber.getCertificationNumber();

            //메일 전송
            boolean isSuccessed = emailProvider.sendCertificationMail(email, certificationNumber);
            if(!isSuccessed) return EmailCertificationResponseDto.mailSendFail();

            //메일 전송 결과 저장
            CertificationEntity certificationEntity = new CertificationEntity(userId, email, certificationNumber);
            certificationRepository.save(certificationEntity);


        }catch (Exception exception){
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }

        return EmailCertificationResponseDto.success();

    }

    @Override
    public ResponseEntity<? super CheckCertificationResponseDto> checkCertification(CheckCertificationRequestDto dto) {
        try{

            String userId = dto.getId();
            String email = dto.getEmail();
            String certificationNumber = dto.getCertificationNumber();

            CertificationEntity certificationEntity = certificationRepository.findByUserId(userId);
            if(certificationEntity == null) return CheckCertificationResponseDto.certificationFail();

            boolean isMatched = certificationEntity.getEmail().equals(email) && certificationEntity.getCertificationNumber().equals(certificationNumber);
            if(!isMatched) return CheckCertificationResponseDto.certificationFail();

        } catch (Exception exception){
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }
        return CheckCertificationResponseDto.success();
    }

    @Override
    public ResponseEntity<? super SignUpResponseDto> signUp(SignUpRequestDto dto) {
        try{

            //존재하는 유저아이디인지 체크
            String userId = dto.getId();
            boolean isExistId = userRepository.existsByUserId(userId);
            if(isExistId)return SignUpResponseDto.duplicateId();

            //이메일 체크하는데 엔티티를 userId로 불러와서 체크하고 email과 number값이 재대로 같은지 체크
            String email = dto.getEmail();
            String certificationNumber = dto.getCertificationNumber();
            CertificationEntity certificationEntity = certificationRepository.findByUserId(userId);
            boolean isMatched = certificationEntity.getEmail().equals(email) && certificationEntity.getCertificationNumber().equals(certificationNumber);
            if(!isMatched) return SignUpResponseDto.certificationFail();

            //패스워드 체크
            String password = dto.getPassword();
            //암호화 시켜줄거임
            String encodedPassword = passwordEncoder.encode(password);
            dto.setPassword(encodedPassword);

            UserEntity userEntity = new UserEntity(dto);
            userRepository.save(userEntity);

            certificationRepository.deleteByUserId(userId);

        }catch (Exception exception){
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }
        return SignUpResponseDto.success();
    }

    @Override
    public ResponseEntity<? super SignInResponseDto> signIn(SignInRequestDto dto) {

        String token = null;

        try{

            String userId = dto.getId();
            UserEntity userEntity = userRepository.findByUserId(userId);
            if(userEntity == null) return SignInResponseDto.signInFail();

            String password = dto.getPassword();
            String encodedPassword = userEntity.getPassword();
            boolean isMatched = passwordEncoder.matches(password, encodedPassword);
            if(!isMatched) return SignInResponseDto.signInFail();

            //토큰 생성
            token = jwtProvider.create(userId);


        }catch (Exception exception){
            exception.printStackTrace();
            return ResponseDto.databaseError();
        }

        return SignInResponseDto.success(token);

    }

@Override
public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response, String loginMethod) {
    String token = null;
    Cookie[] cookies = request.getCookies();

    // 세션을 무효화하는 방법
    HttpSession session = request.getSession(false); // 세션이 있으면 반환, 없으면 null 반환
    if (session != null) {
        session.invalidate(); // 세션 무효화
    }

    // 세션 관련 쿠키 삭제
    Cookie sessionCookie = new Cookie("Idea-b5e63b4f", null);  // 세션 쿠키 이름
    sessionCookie.setMaxAge(0);  // 쿠키 만료
    sessionCookie.setDomain("localhost:4040"); // 도메인 설정
    sessionCookie.setPath("/");   // 도메인 범위 설정
    // sessionCookie.setDomain("127.0.0.1"); // 127.0.0.1로도 시도해 보세요
    sessionCookie.setSecure(false); // 로컬 환경에서는 Secure를 false로 설정 (HTTPS 환경이 아닌 경우)
    sessionCookie.setHttpOnly(true); // HttpOnly 설정을 통해 클라이언트에서 접근을 막을 수 있음
    response.addCookie(sessionCookie);

    // SameSite 설정을 위한 응답 헤더 추가
//    response.setHeader("Set-Cookie", "Idea-b5e63b4f=; Max-Age=0; Path=/; Domain=localhost; Secure=false; HttpOnly; SameSite=None");


    // 각 로그인 방식에 따른 토큰을 찾기
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if ("kakaoToken".equals(cookie.getName()) && "kakao".equals(loginMethod)) {
                token = cookie.getValue();
                break;
            } else if ("naverToken".equals(cookie.getName()) && "naver".equals(loginMethod)) {
                token = cookie.getValue();
                break;
            }
        }
    }

    if (token == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loginMethod + " 토큰이 없습니다.");
    }

    // 로그인 방식에 따라 로그아웃 처리
    if ("kakao".equals(loginMethod)) {
        return logoutKakao(token, response);
    } else if ("naver".equals(loginMethod)) {
        return logoutNaver(token, response);
    } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("지원되지 않는 로그인 방식입니다.");
    }
}
    // 카카오 로그아웃
    private ResponseEntity<String> logoutKakao(String kakaoToken, HttpServletResponse response) {
        String KAKAO_LOGOUT_URL = "https://kapi.kakao.com/v1/user/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + kakaoToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> kakaoResponse = restTemplate.exchange(KAKAO_LOGOUT_URL, HttpMethod.POST, entity, String.class);

        if (kakaoResponse.getStatusCode() == HttpStatus.OK) {
            // 쿠키 삭제
            Cookie cookie = new Cookie("kakaoToken", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);

            return ResponseEntity.ok("카카오 로그아웃 성공");
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("카카오 로그아웃 실패");
    }

    // 네이버 로그아웃
    private ResponseEntity<String> logoutNaver(String naverToken, HttpServletResponse response) {
        String NAVER_LOGOUT_URL = "https://nid.naver.com/oauth2.0/token?grant_type=delete&client_id=naver.client.id&client_secret=naver.client.secret&access_token=" + naverToken;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> naverResponse = restTemplate.exchange(NAVER_LOGOUT_URL, HttpMethod.GET, entity, String.class);

        if (naverResponse.getStatusCode() == HttpStatus.OK) {
            // 쿠키 삭제
            Cookie cookie = new Cookie("naverToken", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);

            return ResponseEntity.ok("네이버 로그아웃 성공");
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("네이버 로그아웃 실패");
    }


}
