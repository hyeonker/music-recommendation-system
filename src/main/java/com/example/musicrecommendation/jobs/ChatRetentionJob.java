package com.example.musicrecommendation.jobs;

import com.example.musicrecommendation.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRetentionJob {

    private final ChatMessageService chatMessageService;

    @Value("${app.retention.chat.months:3}")
    private int retentionMonths;

    // 매일 04:00
    @Scheduled(cron = "0 0 4 * * *")
    public void run() {
        int deleted = chatMessageService.purgeOlderThanMonths(retentionMonths);
        log.info("Chat retention: {} messages deleted (>{} months)", deleted, retentionMonths);
    }
}
