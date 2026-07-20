package com.vslbackend.service.inter;

public interface OtpService {
    String generateAndStoreOtp(String email);
    boolean verifyOtp(String email, String otp);
    void clearOtp(String email);
}
