package com.rummy.service;

import com.rummy.dto.UserRegistrationDto;
import com.rummy.model.User;
import com.rummy.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import com.rummy.exception.UserServiceException;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(UserRegistrationDto registrationDto, HttpServletRequest request) {
        // Validate if passwords match
        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            throw new UserServiceException("Passwords do not match");
        }

        // Check if username or mobile number already exists
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new UserServiceException("Username already exists");
        }
        if (userRepository.existsByMobileNumber(registrationDto.getMobileNumber())) {
            throw new UserServiceException("Mobile number already registered");
        }

        // Create new user
        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setMobileNumber(registrationDto.getMobileNumber());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setLastLoginIp(getClientIp(request));
        
        // Generate and set OTP
        String otp = generateOTP();
        user.setOtp(otp);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(10));

        return userRepository.save(user);
    }

    public boolean verifyOTP(String mobileNumber, String otp) {
        User user = userRepository.findByMobileNumber(mobileNumber)
            .orElseThrow(() -> new UserServiceException("User not found"));

        if (user.getOtp() == null || user.getOtpExpiryTime() == null) {
            throw new UserServiceException("No OTP request found");
        }

        if (LocalDateTime.now().isAfter(user.getOtpExpiryTime())) {
            throw new UserServiceException("OTP has expired");
        }

        if (!user.getOtp().equals(otp)) {
            throw new UserServiceException("Invalid OTP");
        }

        user.setVerified(true);
        user.setOtp(null);
        user.setOtpExpiryTime(null);
        userRepository.save(user);
        return true;
    }

    private String generateOTP() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}