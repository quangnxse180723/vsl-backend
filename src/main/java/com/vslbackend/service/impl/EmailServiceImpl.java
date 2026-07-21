package com.vslbackend.service.impl;

import com.vslbackend.service.inter.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.error("Khong tim thay RESEND_API_KEY. Vui long cau hinh bien moi truong.");
            return;
        }

        try {
            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                    <h2 style="color: #4f46e5; text-align: center;">SignMentor</h2>
                    <p>Chào bạn,</p>
                    <p>Mã OTP xác thực của bạn là:</p>
                    <div style="background-color: #f3f4f6; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; border-radius: 8px; margin: 20px 0;">
                        %s
                    </div>
                    <p style="color: #6b7280; font-size: 14px;">Mã OTP này có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>
                    <p>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.</p>
                    <br/>
                    <p>Trân trọng,<br/>Đội ngũ SignMentor</p>
                </div>
                """.formatted(otp);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            // Resend only allows sending FROM onboarding@resend.dev until you verify a domain.
            // And you can only send TO the email address you registered Resend with.
            Map<String, Object> body = Map.of(
                    "from", "SignMentor <onboarding@resend.dev>",
                    "to", List.of(toEmail),
                    "subject", "Mã Xác Thực OTP - SignMentor",
                    "html", htmlContent
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity("https://api.resend.com/emails", entity, String.class);
            log.info("Da gui email OTP thanh cong den: {} qua Resend API", toEmail);

        } catch (Exception e) {
            log.error("Loi khi gui email OTP den {} qua Resend API: {}", toEmail, e.getMessage());
        }
    }
}

