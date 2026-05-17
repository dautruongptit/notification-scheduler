package com.app.notification.scheduler.Repository;

import com.app.notification.scheduler.Entity.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<NotificationRecord, Long> {

    /**
     * Lay cac ban ghi can gui thong bao:
     * - Status: PENDING (moi) hoac FAILED (cho retry)
     * - Thoi diem notify_at da den (<=  now)
     * - Chua vuot qua so lan retry cho phep
     */
    @Query("""
    SELECT n FROM NotificationRecord n
    WHERE n.status IN (
        com.app.notification.scheduler.Entity.NotificationRecord.NotifStatus.PENDING,
        com.app.notification.scheduler.Entity.NotificationRecord.NotifStatus.FAILED
    )
      AND n.notifyAt <= :now
      AND n.retryCount < :maxRetry
    ORDER BY n.notifyAt ASC
""")
    List<NotificationRecord> findReadyToSend(
            @Param("now") LocalDateTime now,
            @Param("maxRetry") int maxRetry
    );
}