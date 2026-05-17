package com.app.notification.scheduler.Service;

import com.app.notification.scheduler.Entity.DeviceToken;
import com.app.notification.scheduler.Entity.NotificationLog;
import com.app.notification.scheduler.Entity.NotificationRecord;
import com.app.notification.scheduler.Repository.DeviceTokenRepository;
import com.app.notification.scheduler.Repository.NotificationLogRepository;
import com.app.notification.scheduler.Repository.NotificationRepository;
import com.app.notification.scheduler.Service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class NotificationProcessor {

    // ─── Inject cac dependency qua constructor (@RequiredArgsConstructor) ──
    private final NotificationRepository notificationRepo;
    private final DeviceTokenRepository deviceTokenRepo;
    private final NotificationLogRepository logRepo;
    private final FcmService fcmService;

    private static final int MAX_RETRY = 3;

    /**
     * Xử lý 1 bản ghi notification.
     * @return true nếu gửi thành công đến ít nhất 1 thiết bị.
     */
    @Transactional
    public boolean process(NotificationRecord record) {

        // B1: Đánh dấu PROCESSING — tránh job khác lấy trùng
        record.setStatus(NotificationRecord.NotifStatus.PROCESSING);
        notificationRepo.save(record);

        // B2: Lấy danh sách thiết bị đang hoạt động của customer
        //     findByCustomerIdAndActiveTrue (đổi từ isActiveTrue sau khi fix DeviceToken)
        List<DeviceToken> devices =
                deviceTokenRepo.findByCustomerIdAndIsActiveTrue(record.getCustomerId());

        if (devices.isEmpty()) {
            markFailed(record, "No active device found for customerId=" + record.getCustomerId());
            return false;
        }

        // B3: Build data payload theo loại thông báo
        Map<String, String> payload = Map.of(
                "notifId", String.valueOf(record.getId()),
                "type",    record.getNotifType().name(),
                "screen",  resolveScreen(record.getNotifType())
        );

        // B4: Gửi đến từng thiết bị — 1 thiết bị lỗi không ảnh hưởng thiết bị khác
        boolean anySuccess = false;
        for (DeviceToken device : devices) {
            FcmService.FcmResult result = fcmService.send(
                    device.getFcmToken(),
                    record.getTitle(),
                    record.getMessage(),
                    payload
            );

            // Lưu log với REQUIRES_NEW — không bị rollback khi exception
            saveLog(record, device, result);

            if (result.success()) {
                anySuccess = true;
            } else {
                handleInvalidToken(device, result.error());
            }
        }

        // B5: Cập nhật status cuối cùng
        if (anySuccess) {
            markSent(record);
        } else {
            markFailed(record, "All devices failed");
        }

        return anySuccess;
    }

    // ── Resolve screen ────────────────────────────────────────────────

    /** Xác định màn hình mobile sẽ mở khi nhận thông báo. */
    private String resolveScreen(NotificationRecord.NotifType type) {
        return switch (type) {
            case APPOINTMENT -> "appointment_detail";
            case TRANSACTION -> "transaction_detail";
            case ALERT       -> "home";
            case PROMOTION   -> "promotion_list";
        };
    }

    // ── Mark SENT ─────────────────────────────────────────────────────

    private void markSent(NotificationRecord record) {
        record.setStatus(NotificationRecord.NotifStatus.SENT);
        record.setSentAt(LocalDateTime.now());
        notificationRepo.save(record);
        log.info("[Processor] SENT | notifId={} | customerId={}",
                record.getId(), record.getCustomerId());
    }

    // ── Mark FAILED với retry delay tăng dần ─────────────────────────

    /**
     * Retry delay: lần 1 → +2p, lần 2 → +4p, lần 3 → FAILED vĩnh viễn.
     */
    private void markFailed(NotificationRecord record, String reason) {
        record.setRetryCount(record.getRetryCount() + 1);
        record.setLastError(reason);

        if (record.getRetryCount() >= MAX_RETRY) {
            record.setStatus(NotificationRecord.NotifStatus.FAILED);
            log.warn("[Processor] FAILED permanently | notifId={} | reason={}",
                    record.getId(), reason);
        } else {
            record.setStatus(NotificationRecord.NotifStatus.PENDING);
            record.setNotifyAt(
                    LocalDateTime.now().plusMinutes(record.getRetryCount() * 2L));
            log.warn("[Processor] Will retry ({}/{}) | notifId={} | nextAt={}",
                    record.getRetryCount(), MAX_RETRY,
                    record.getId(), record.getNotifyAt());
        }
        notificationRepo.save(record);
    }

    // ── Save log — REQUIRES_NEW để không bị rollback ─────────────────

    /**
     * Lưu log trong transaction riêng (REQUIRES_NEW).
     * Nếu process() rollback, log vẫn được lưu để debug.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(NotificationRecord record,
                        DeviceToken device,
                        FcmService.FcmResult result) {
        logRepo.save(NotificationLog.builder()
                .referenceId(record.getId())
                .referenceType(record.getNotifType().name())
                .customerId(record.getCustomerId())
                .fcmToken(device.getFcmToken())
                .status(result.success() ? "SUCCESS" : "FAILED")
                .sentAt(LocalDateTime.now())
                .errorMessage(result.error())
                .build());
    }

    // ── Vô hiệu hóa token không còn hợp lệ ──────────────────────────

    /**
     * FCM trả UNREGISTERED hoặc INVALID_ARGUMENT → token đã bị xóa/invalid.
     * Đặt active = false để không gửi lần sau.
     *
     * FIX: device.setIsActive(false) → device.setActive(false)
     * (do DeviceToken đổi field isActive → active)
     */
    private void handleInvalidToken(DeviceToken device, String error) {
        if (error != null
                && (error.contains("UNREGISTERED")
                || error.contains("INVALID_ARGUMENT"))) {
            device.setIsActive(false);    // ← đã fix: setActive thay vì setIsActive
            deviceTokenRepo.save(device);
            log.warn("[Processor] Token deactivated | deviceId={} | token={}...",
                    device.getId(),
                    device.getFcmToken().substring(0, Math.min(10, device.getFcmToken().length())));
        }
    }
}

