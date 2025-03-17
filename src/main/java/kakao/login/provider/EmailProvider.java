package kakao.login.provider;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailProvider {

    private final JavaMailSender javaMailSender;

    private final String SUBJECT = "[병원 조직관리 서비스] 인증메일입니다."; // 인증메일 제목

    // 인증 메일을 전송하는 메서드
    public boolean sendCertificationMail(String email, String certificationNumber){

        try{
            // 메일 메시지 생성
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true);

            // HTML 형식의 인증 메시지 생성
            String htmlContent = getCertificationMessage(certificationNumber);

            // 메일 발송
            messageHelper.setTo(email);
            messageHelper.setSubject(SUBJECT);
            messageHelper.setText(htmlContent, true);

            javaMailSender.send(message);  // 메일 전송

        } catch (Exception exception){
            exception.printStackTrace();
            return false;  // 실패 시 false 반환
        }

        return true;  // 성공 시 true 반환
    }

    // 인증 메일 내용 생성
    private String getCertificationMessage(String certificationNumber){

        String certificationMessage = "";
        certificationMessage += "<h1 style='text-align: center;'>[병원 조직관리 서비스] 인증메일</h1>";
        certificationMessage += "<h3 style='text-align: center;'>인증코드 : <strong style='font-size: 32px; letter-spacing: 8px;'>" + certificationNumber + "</strong></h3>";
        return certificationMessage;
    }
}
