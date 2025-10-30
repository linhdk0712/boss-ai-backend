package ai.content.auto.service;

import ai.content.auto.annotation.Loggable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@bossai.com.vn}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Loggable
    public void sendVerificationEmail(String toEmail, String firstName, String verificationToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Xác thực tài khoản Boss AI");

            String emailBody = getEmailBody(firstName, verificationToken);

            message.setText(emailBody);
            mailSender.send(message);

            log.info("Verification email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    private String getEmailBody(String firstName, String verificationToken) {
        String activationUrl = frontendUrl + "/auth/activate?token=" + verificationToken;
        String emailBody = String.format(
                "Xin chào %s,\n\n" +
                        "Cảm ơn bạn đã đăng ký tài khoản Boss AI!\n\n" +
                        "Để hoàn tất quá trình đăng ký, vui lòng nhấp vào liên kết bên dưới để xác thực email của bạn:\n\n"
                        +
                        "%s\n\n" +
                        "Liên kết này sẽ hết hạn sau 24 giờ.\n\n" +
                        "Nếu bạn không đăng ký tài khoản này, vui lòng bỏ qua email này.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ Boss AI",
                firstName != null ? firstName : "Bạn",
                activationUrl);
        return emailBody;
    }
}