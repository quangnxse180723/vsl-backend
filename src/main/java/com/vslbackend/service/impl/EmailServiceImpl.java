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

    @Value("${RESEND_FROM_EMAIL:SignMentor <onboarding@resend.dev>}")
    private String fromEmail;

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

            // Resend requires the "from" email to be a verified domain, or onboarding@resend.dev for testing.
            Map<String, Object> body = Map.of(
                    "from", fromEmail,
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

    @Async
    @Override
    public void sendStreakReminderEmail(String toEmail, String fullName, int currentStreak) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("RESEND_API_KEY chua duoc cau hinh. Bo qua gui email nhac nho streak.");
            return;
        }
        try {
            String displayName = (fullName != null && !fullName.isBlank()) ? fullName : "b\u1ea1n";
            String fireEmojis = "\uD83D\uDD25".repeat(Math.min(currentStreak, 5));
            String htmlContent = """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #0A0E1A; border-radius: 16px; overflow: hidden;">
                  <div style="background: linear-gradient(135deg, #4f46e5 0%%, #7c3aed 100%%); padding: 40px 36px; text-align: center;">
                    <div style="font-size: 56px; margin-bottom: 8px;">%s</div>
                    <h1 style="color: #fff; font-size: 26px; margin: 0; font-weight: 800;">Ch\u01b0\u1eddi \u0111\u1ed3ng h\u00e0nh SignMentor!</h1>
                    <p style="color: rgba(255,255,255,0.8); margin: 8px 0 0; font-size: 15px;">\u0110\u1eebng \u0111\u1ec3 chu\u1ed7i h\u1ecdc c\u1ee7a b\u1ea1n b\u1ecb g\u00e3y!</p>
                  </div>
                  <div style="padding: 36px; background: #111827;">
                    <p style="color: #e5e7eb; font-size: 16px; line-height: 1.7; margin: 0 0 20px;">Xin ch\u00e0o <strong style="color: #a78bfa;">%s</strong>,</p>
                    <p style="color: #9ca3af; font-size: 15px; line-height: 1.7; margin: 0 0 24px;">
                      B\u1ea1n \u0111ang c\u00f3 chu\u1ed7i h\u1ecdc t\u1eadp li\u00ean ti\u1ebfp <strong style="color: #f59e0b;">%d ng\u00e0y</strong> r\u1ea5t \u1ea5n t\u01b0\u1ee3ng! 
                      Nh\u01b0ng hi\u1ec7n b\u1ea1n ch\u01b0a luy\u1ec7n t\u1eadp h\u00f4m nay v\u00e0 ch\u01b0a c\u00f2n nhi\u1ec1u gi\u1edd \u0111\u1ec3 gi\u1eef chu\u1ed7i n\u00e0y.
                    </p>
                    <div style="background: linear-gradient(135deg, rgba(79,70,229,0.15) 0%%, rgba(124,58,237,0.15) 100%%); border: 1px solid rgba(79,70,229,0.4); border-radius: 12px; padding: 20px; text-align: center; margin-bottom: 28px;">
                      <div style="font-size: 48px; font-weight: 900; color: #f59e0b;">%d</div>
                      <div style="color: #9ca3af; font-size: 13px; text-transform: uppercase; letter-spacing: 2px;">Ng\u00e0y li\u00ean ti\u1ebfp</div>
                    </div>
                    <div style="text-align: center; margin-bottom: 28px;">
                      <a href="https://sighmentor.click" 
                         style="display: inline-block; background: linear-gradient(135deg, #4f46e5 0%%, #7c3aed 100%%); color: #fff; text-decoration: none; font-weight: 700; font-size: 16px; padding: 14px 36px; border-radius: 10px; box-shadow: 0 4px 24px rgba(79,70,229,0.4);">
                        \uD83D\uDCDA Luy\u1ec7n t\u1eadp ngay
                      </a>
                    </div>
                    <p style="color: #6b7280; font-size: 13px; text-align: center; line-height: 1.6;">
                      B\u1ea1n nh\u1eadn \u0111\u01b0\u1ee3c email n\u00e0y v\u00ec \u0111\u00e3 b\u1eadt th\u00f4ng b\u00e1o nh\u1eafc nh\u1edf tr\u00ean SignMentor.<br/>
                      \u0110\u1ec3 t\u1eaft th\u00f4ng b\u00e1o, v\u00e0o <strong>Trang c\u00e1 nh\u00e2n &rarr; C\u00e0i \u0111\u1eb7t th\u00f4ng b\u00e1o</strong>.
                    </p>
                  </div>
                </div>
                """.formatted(fireEmojis, displayName, currentStreak, currentStreak);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, Object> body = Map.of(
                    "from", fromEmail,
                    "to", List.of(toEmail),
                    "subject", String.format("\uD83D\uDD25 Chu\u1ed7i %d ng\u00e0y c\u1ee7a b\u1ea1n s\u1eafp b\u1ecb g\u00e3y! - SignMentor", currentStreak),
                    "html", htmlContent
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity("https://api.resend.com/emails", entity, String.class);
            log.info("Da gui email nhac nho streak ({} ngay) cho: {}", currentStreak, toEmail);
        } catch (Exception e) {
            log.error("Loi gui email nhac nho streak cho {}: {}", toEmail, e.getMessage());
        }
    }
}

