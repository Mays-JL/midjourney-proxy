package com.github.novicezk.midjourney.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AccountTimeTracker {
    private static final ConcurrentHashMap<String, LocalDateTime> accountStartTimes = new ConcurrentHashMap<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 记录账号启用时间
     */
    public static void recordAccountEnabled(String accountId) {
        LocalDateTime now = LocalDateTime.now();
        accountStartTimes.put(accountId, now);
        log.info("[{}]账号启用时间：{}",accountId, now.format(formatter));
    }

    /**
     * 记录账号禁用时间并计算使用时长
     */
    public static void recordAccountDisabled(String accountId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = accountStartTimes.get(accountId);

        log.info("[{}]账号禁用时间：{}",accountId, now.format(formatter));

        if (startTime != null) {
            Duration duration = Duration.between(startTime, now);
            long minutes = duration.toMinutes();
            long seconds = duration.getSeconds() % 60;

            log.info("账号 [{}] 使用时长：{}分钟{}秒", accountId, minutes, seconds);

            // 清理已禁用账号的记录
            accountStartTimes.remove(accountId);
        } else {
            log.warn("账号 [{}] 未找到启用时间记录", accountId);
        }
    }

}
