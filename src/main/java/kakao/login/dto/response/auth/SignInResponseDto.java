package kakao.login.dto.response.auth;

import kakao.login.common.ResponseCode;
import kakao.login.common.ResponseMessage;
import kakao.login.dto.response.ResponseDto;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
public class SignInResponseDto extends ResponseDto {

    private String token;
    private String refreshToken;
    private long expiresIn;

    private SignInResponseDto(String code, String message, String token, String refreshToken, long expiresIn) {
        super(code, message);
        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    // 로그인 성공 응답
    public static ResponseEntity<SignInResponseDto> success(String token, String refreshToken, long expiresIn) {
        SignInResponseDto responseBody = new SignInResponseDto(ResponseCode.SUCCESS, ResponseMessage.SUCCESS, token, refreshToken, expiresIn);
        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }


    // 로그인 실패 응답
    public static ResponseEntity<ResponseDto> signInFail() {
        ResponseDto responseBody = new ResponseDto(ResponseCode.SIGN_IN_FAIL, ResponseMessage.SIGN_IN_FAIL);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
    }
}