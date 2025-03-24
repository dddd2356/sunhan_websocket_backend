package kakao.login.dto.response.auth;


import kakao.login.common.ResponseCode;
import kakao.login.common.ResponseMessage;
import kakao.login.dto.response.ResponseDto;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
public class LogOutResponseDto extends ResponseDto {

    public static ResponseEntity<LogOutResponseDto> success(){
        LogOutResponseDto responseBody = new LogOutResponseDto();
        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }
    public static ResponseEntity<ResponseDto> logOutFail(){
        ResponseDto responseBody = new ResponseDto(ResponseCode.LOGOUT_FAIL, ResponseMessage.LOGOUT_FAIL);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
    }


}