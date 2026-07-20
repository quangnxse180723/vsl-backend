package com.vslbackend.service.impl;

import com.vslbackend.service.inter.OtpService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpServiceImpl implements OtpService {

    private static final long OTP_EXPIRATION_TIME_MS = 5 * 60 * 1000; // 5 minutes
    
    // Structure to store OTP and Expiration Time
    private static class OtpDetails {
        String otp;
        long expiryTime;

        OtpDetails(String otp, long expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }

    private final Map<String, OtpDetails> otpCache = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateAndStoreOtp(String email) {
        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        long expiryTime = System.currentTimeMillis() + OTP_EXPIRATION_TIME_MS;
        otpCache.put(email, new OtpDetails(otp, expiryTime));
        return otp;
    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        OtpDetails details = otpCache.get(email);
        if (details == null) {
            return false;
        }
        
        if (System.currentTimeMillis() > details.expiryTime) {
            otpCache.remove(email);
            return false;
        }
        
        return details.otp.equals(otp);
    }

    @Override
    public void clearOtp(String email) {
        otpCache.remove(email);
    }
}
