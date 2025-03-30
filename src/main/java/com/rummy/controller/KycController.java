package com.rummy.controller;

import com.rummy.model.KycStatus;
import com.rummy.service.KycService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kyc")
public class KycController {
    @Autowired
    private KycService kycService;

    @PutMapping("/{userId}/status")
    public ResponseEntity<?> updateKycStatus(
            @PathVariable Long userId,
            @RequestParam KycStatus status) {
        try {
            return ResponseEntity.ok(kycService.updateUserKycStatus(userId, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{userId}/status")
    public ResponseEntity<?> getKycStatus(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(kycService.getKycStatus(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}