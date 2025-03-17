package kakao.login.dto.response.auth;

import kakao.login.common.ResponseCode;
import kakao.login.common.ResponseMessage;
import kakao.login.dto.response.ResponseDto;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
// 로그아웃 응답 DTO
public class LogOutResponseDto extends ResponseDto {

    // 로그아웃 성공 응답
    public static ResponseEntity<LogOutResponseDto> success(){
        LogOutResponseDto responseBody = new LogOutResponseDto();
        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }
}