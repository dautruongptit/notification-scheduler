package com.app.notification.scheduler.run;

import com.app.notification.scheduler.Entity.NotificationRecord;
import com.app.notification.scheduler.Repository.NotificationRepository;
import com.app.notification.scheduler.Service.NotificationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationRepository notificationRepo;
    private final NotificationProcessor processor;

    private static final int MAX_RETRY  = 3;
    private static final int BATCH_SIZE = 50;

    /**
     * fixedDelay  = dem tu luc job KET THUC
     * initialDelay = doi 5s truoc khi chay lan dau
     * -> Khong bao gio chay chong len nhau
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 5_000)
    public void scan() {
        log.debug("[Scheduler] Scanning at {}", LocalDateTime.now());

        List<NotificationRecord> records = notificationRepo
                .findReadyToSend(LocalDateTime.now(), MAX_RETRY);

        // Gioi han batch de tranh xu ly qua nhieu 1 luc
        List<NotificationRecord> batch = records.stream()
                .limit(BATCH_SIZE).toList();

        if (batch.isEmpty()) return;

        log.info("[Scheduler] Found {} records.", batch.size());
        int success = 0, failed = 0;

        for (NotificationRecord record : batch) {
            try {
                // Xu ly tung ban ghi doc lap
                // 1 ban ghi loi khong anh huong ban ghi khac
                boolean ok = processor.process(record);
                if (ok) success++; else failed++;
            } catch (Exception e) {
                failed++;
                log.error("[Scheduler] Error notifId={} : {}",
                        record.getId(), e.getMessage(), e);
            }
        }

        log.info("[Scheduler] Done | success={} failed={}",
                success, failed);
    }
}
