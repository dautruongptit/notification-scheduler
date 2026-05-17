package com.app.notification.scheduler.Repository;

import com.app.notification.scheduler.Entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationLogRepository
        extends JpaRepository<NotificationLog, Long> {

    /**
     * Kiểm tra đã từng gửi thành công chưa (tránh gửi trùng lặp).
     *
     * Ví dụ dùng:
     *   boolean alreadySent = logRepo.existsByReferenceIdAndReferenceTypeAndStatus(
     *       record.getId(), record.getNotifType().name(), "SUCCESS"
     *   );
     */
    boolean existsByReferenceIdAndReferenceTypeAndStatus(
            Long referenceId, String referenceType, String status);
}
