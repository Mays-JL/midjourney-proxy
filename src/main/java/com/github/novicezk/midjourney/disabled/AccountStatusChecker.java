package com.github.novicezk.midjourney.disabled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class AccountStatusChecker {

    @Autowired
    private SinglediscordAccountInitializer singlediscordAccountInitializer;

    @Scheduled(fixedRate = 3600000) // 5分钟 = 300000毫秒
    public void checkDisabledAccounts() {
        log.info("===定时任务开始===");
        singlediscordAccountInitializer.fun();
    }
}