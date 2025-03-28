package kakao.login.dto.request.message;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkMessageRequestDto {
    private String message;
    private List<Long> employeeIds;
}