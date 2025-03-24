// TokenRefreshResponseDto.java
package kakao.login.dto.response.auth;

import kakao.login.dto.response.ResponseDto;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
public class RefreshTokenResponseDto extends ResponseDto {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;

    public RefreshTokenResponseDto(String accessToken, String refreshToken, long expiresIn) {
        super("SU", "Success");
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public static ResponseEntity<RefreshTokenResponseDto> success(String accessToken, String refreshToken, long expiresIn) {
        RefreshTokenResponseDto responseBody = new RefreshTokenResponseDto(accessToken, refreshToken, expiresIn);
        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }

    public static ResponseEntity<ResponseDto> refreshTokenNotProvided() {
        ResponseDto responseBody = new ResponseDto("RF", "Refresh token not provided");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
    }

    public static ResponseEntity<ResponseDto> invalidRefreshToken() {
        ResponseDto responseBody = new ResponseDto("RF", "Invalid refresh token");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
    }

    public static ResponseEntity<ResponseDto> userNotFound() {
        ResponseDto responseBody = new ResponseDto("NF", "User not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseBody);
    }
}