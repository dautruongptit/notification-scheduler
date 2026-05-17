package com.app.notification.scheduler.Repository;

import com.app.notification.scheduler.Entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn bảng DEVICE_TOKENS.
 *
 * FIX: Tên method đổi từ findByCustomerIdAndIsActiveTrue
 *      → findByCustomerIdAndActiveTrue
 *
 * Lý do: Entity DeviceToken đổi field "isActive" → "active"
 * để fix lỗi Lombok setter. Spring Data JPA sinh query từ tên field
 * trong entity, nên method name phải khớp tên field.
 *
 * Spring Data JPA tự sinh SQL:
 *   SELECT * FROM device_tokens
 *   WHERE customer_id = ? AND is_active = 1
 */
@Repository
public interface DeviceTokenRepository
        extends JpaRepository<DeviceToken, Long> {

    /**
     * Tìm tất cả thiết bị đang hoạt động của 1 customer.
     * Dùng trong NotificationProcessor để lấy FCM token.
     */
    List<DeviceToken> findByCustomerIdAndIsActiveTrue(Long customerId);
    // Thêm 2 method này để DeviceTokenController dùng
    Optional<DeviceToken> findByFcmToken(String fcmToken);

    void deleteByCustomerIdAndIsActiveFalse(Long customerId);

    // Dùng khi logout: vô hiệu hóa theo customerId
    @Modifying
    @Query("UPDATE DeviceToken d SET d.isActive = false, " +
            "d.updatedAt = :now WHERE d.customerId = :cid")
    void deactivateAllByCustomerId(@Param("cid")  Long customerId,
                                   @Param("now") LocalDateTime now);
}

