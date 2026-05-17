package com.app.notification.scheduler.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notif_type", nullable = false, length = 50)
    private NotifType notifType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotifStatus status;

    @Column(name = "notify_at", nullable = false)
    private LocalDateTime notifyAt;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
    public enum NotifStatus {
        PENDING,        // Chờ gửi hoặc chờ retry
        PROCESSING,     // Đang được Scheduler xử lý
        SENT,           // Đã gửi thành công
        FAILED          // Thất bại, đã hết số lần retry
    }

    public enum NotifType {
        APPOINTMENT,
        TRANSACTION,
        ALERT,
        PROMOTION
    }
}
