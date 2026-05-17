package com.app.notification.scheduler.Service;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class FcmService {


    /**
     * Kết quả gửi FCM — dùng Record để tránh boilerplate.
     *   result.success()   → true / false
     *   result.messageId() → ID trả về từ Firebase khi thành công
     *   result.error()     → mã lỗi + message khi thất bại
     */
    public record FcmResult(boolean success, String messageId, String error) {}

    /**
     * Gửi push notification đến 1 FCM token cụ thể.
     *
     * @param fcmToken Token thiết bị (lấy từ bảng DEVICE_TOKENS)
     * @param title    Tiêu đề hiển thị trên điện thoại
     * @param body     Nội dung thông báo
     * @param data     Data payload bổ sung (notifId, type, screen...)
     * @return FcmResult chứa kết quả gửi
     */
    public FcmResult send(String fcmToken,
                          String title,
                          String body,
                          Map<String, String> data) {
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    // Notification hiển thị trên notification tray
                    .setNotification(
                            Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build()
                    )
                    // Data payload — app xử lý khi nhận thông báo
                    .putAllData(data != null ? data : Map.of())
                    // Android: HIGH priority để hiện ngay khi màn hình tắt
                    .setAndroidConfig(
                            AndroidConfig.builder()
                                    .setPriority(AndroidConfig.Priority.HIGH)
                                    .build()
                    )
                    // iOS: âm thanh mặc định
                    .setApnsConfig(
                            ApnsConfig.builder()
                                    .setAps(Aps.builder()
                                            .setSound("default")
                                            .build())
                                    .build()
                    )
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            log.debug("[FCM] Success | token={}... | messageId={}",
                    fcmToken.substring(0, Math.min(10, fcmToken.length())), messageId);

            return new FcmResult(true, messageId, null);

        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Failed | code={} | error={}",
                    e.getMessagingErrorCode(), e.getMessage());

            return new FcmResult(false, null,
                    e.getMessagingErrorCode() + ": " + e.getMessage());
        }
    }
}
