package com.vslbackend.service.inter;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otp);
}
