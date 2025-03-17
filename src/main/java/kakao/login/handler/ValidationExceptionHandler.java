package kakao.login.handler;

import kakao.login.dto.response.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice  // 모든 컨트롤러에서 발생하는 예외를 처리하는 클래스
public class ValidationExceptionHandler {

    // MethodArgumentNotValidException 또는 HttpMessageNotReadableException이 발생했을 때 호출
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ResponseDto> validationExceptionHandler(Exception exception){
        // 검증 실패에 대한 응답을 반환 (커스텀 ResponseDto 활용)
        return ResponseDto.validationFail();
    }
}
