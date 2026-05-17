package com.app.notification.scheduler.controller;

import com.app.notification.scheduler.Entity.DeviceToken;
import com.app.notification.scheduler.Repository.DeviceTokenRepository;
import com.app.notification.scheduler.đto.DeviceTokenRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

// controller/DeviceTokenController.java
@RestController
@RequestMapping("/api/device-tokens")
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenController {

    private final DeviceTokenRepository deviceTokenRepo;

    // ─── Đăng ký / cập nhật FCM token ─────────────────────────────────────

        // ─── Đăng ký / cập nhật FCM token ─────────────────────────────────────
    @PostMapping
    public ResponseEntity<Map<String, String>> register(
            @Validated @RequestBody DeviceTokenRequest req
    ) {
        deviceTokenRepo.findByFcmToken(req.getFcmToken())
                .ifPresentOrElse(
                        existing -> {
                            existing.setIsActive(true);
                            existing.setUpdatedAt(LocalDateTime.now());
                            deviceTokenRepo.save(existing);
                            log.info("[Token] Updated | cid={} | platform={}",
                                    req.getCustomerId(), req.getPlatform());
                        },
                        () -> {
                            deviceTokenRepo.save(DeviceToken.builder()
                                    .customerId(req.getCustomerId())
                                    .fcmToken(req.getFcmToken())
                                    .platform(req.getPlatform())
                                    .deviceName(req.getDeviceName())
                                    .isActive(true)
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build());
                            log.info("[Token] Registered | cid={} | platform={}",
                                    req.getCustomerId(), req.getPlatform());
                        }
                );

        return ResponseEntity.ok(Map.of("status", "registered"));
    }

    // ─── DELETE /api/device-tokens?fcmToken=xxx — Logout 1 thiết bị ───────
    @DeleteMapping
    public ResponseEntity<Map<String, String>> unregister(
            @RequestParam String fcmToken) {

        deviceTokenRepo.findByFcmToken(fcmToken).ifPresent(t -> {
            t.setIsActive(false);
            t.setUpdatedAt(LocalDateTime.now());
            deviceTokenRepo.save(t);
            log.info("[Token] Deactivated | id={}", t.getId());
        });

        return ResponseEntity.ok(Map.of("status", "unregistered"));
    }

    // ─── DELETE /api/device-tokens/all?customerId=xxx — Logout mọi thiết bị
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, String>> unregisterAll(
            @RequestParam Long customerId) {

        deviceTokenRepo.deactivateAllByCustomerId(
                customerId, LocalDateTime.now());
        log.info("[Token] All deactivated | cid={}", customerId);

        return ResponseEntity.ok(Map.of("status", "all_unregistered"));
    }
}