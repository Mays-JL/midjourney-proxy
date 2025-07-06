package com.github.novicezk.midjourney.disabled;

import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.support.DiscordAccountInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class AccountStatusChecker {

    @Autowired
    private DisabledAccountQueue disabledAccountQueue;

    @Autowired
    private SinglediscordAccountInitializer singlediscordAccountInitializer;
    @Autowired
    private DiscordAccountInitializer discordAccountInitializer;

    @Scheduled(fixedRate = 600000) // 5分钟 = 300000毫秒
    public void checkDisabledAccounts() {
        log.info("===定时任务开始===");
        singlediscordAccountInitializer.fun();
    }
}