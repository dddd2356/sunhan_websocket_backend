package kakao.login.dto.response.auth;

import kakao.login.common.ResponseCode;
import kakao.login.common.ResponseMessage;
import kakao.login.dto.response.ResponseDto;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
// 로그인 응답 DTO
public class SignInResponseDto extends ResponseDto {

    private String token;            // 로그인 성공 시 반환되는 토큰
    private int expirationTime;      // 토큰의 만료 시간 (기본 3600초 설정)

    // 생성자
    private SignInResponseDto(String token){
        super();
        this.token = token;
        this.expirationTime = 3600;  // 기본 만료 시간 설정
    }

    // 로그인 성공 시 응답
    public static ResponseEntity<SignInResponseDto> success(String token){
        SignInResponseDto responseBody = new SignInResponseDto(token);
        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }

    // 로그인 실패 응답
    public static ResponseEntity<ResponseDto> signInFail(){
        ResponseDto responseBody = new ResponseDto(ResponseCode.SIGN_IN_FAIL, ResponseMessage.SIGN_IN_FAIL);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
    }
}