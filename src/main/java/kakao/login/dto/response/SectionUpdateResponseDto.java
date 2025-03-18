package kakao.login.dto.response;

import kakao.login.common.ResponseCode;
import kakao.login.common.ResponseMessage;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
public class SectionUpdateResponseDto extends ResponseDto {

    public SectionUpdateResponseDto() {
        super();
    }

    // 섹션 업데이트 성공 응답
    public static ResponseEntity<SectionUpdateResponseDto> success(){
        SectionUpdateResponseDto responseBody = new SectionUpdateResponseDto();
        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }

    // 섹션 업데이트 실패 응답 (예: 섹션 이름이 이미 존재하는 경우)
    public static ResponseEntity<ResponseDto> duplicateId() {
        ResponseDto responseBody = new ResponseDto(ResponseCode.DUPLICATE_ID, ResponseMessage.DUPLICATE_ID);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
    }


}