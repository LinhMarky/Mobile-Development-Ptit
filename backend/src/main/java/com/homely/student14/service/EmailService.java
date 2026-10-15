package com.homely.student14.service;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${homely.mail.from}")
    private String mailFrom;

    public void sendVerificationCode(String email, String code) {
        String content =
                "Mã xác minh email Roomly của bạn: "
                        + code
                        + "\n\nMã có hiệu lực trong 10 phút và chỉ được sử dụng một lần."
                        + "\nNếu bạn không yêu cầu mã này, hãy bỏ qua email.";
        sendEmail(email, "Roomly - Xác minh email", content);
    }

    public void sendRecoveryCode(String email, String code) {
        String content =
                "Mã đặt lại mật khẩu Roomly của bạn: "
                        + code
                        + "\n\n"
                        + "Mã có hiệu lực trong 10 phút và chỉ được sử dụng một lần.\n"
                        + "Không chia sẻ mã với người khác. Nếu bạn không yêu cầu mã này, hãy bỏ"
                        + " qua email.";
        sendEmail(email, "Roomly - Đặt lại mật khẩu", content);
    }

    private void sendEmail(String email, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(content);

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            // Không đưa địa chỉ máy chủ hay thông tin đăng nhập SMTP vào lỗi API.
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Không gửi được email lúc này. Vui lòng thử lại sau.");
        }
    }
}
