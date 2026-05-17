package com.app.notification.scheduler.exception;

/**
 * Ném khi FCM token không còn hợp lệ (UNREGISTERED / INVALID_ARGUMENT).
 * Dùng trong FcmService nếu muốn throw thay vì return FcmResult.
 * Hiện tại NotificationProcessor xử lý inline qua handleInvalidToken().
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String token) {
        super("FCM token invalid or unregistered: " + token);
    }

    public InvalidTokenException(String token, Throwable cause) {
        super("FCM token invalid or unregistered: " + token, cause);
    }
}