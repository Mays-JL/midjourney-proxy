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

    @Scheduled(fixedRate = 10000) // 5分钟 = 300000毫秒
    public void checkDisabledAccounts() {
        log.info("===定时任务开始===");
        singlediscordAccountInitializer.fun();
        //获取所有禁用账号
//        Set<DiscordAccount> disabledAccounts = disabledAccountQueue.getDisabledAccount();
//
//        if (disabledAccounts.isEmpty()) {
//            log.info("===任务结束，当前禁用队列为空===");
//            return;
//        }
//
//        log.info("检查禁用账号状态，当前禁用队列中有 {} 个账号", disabledAccounts.size());
//
//
//        for (DiscordAccount account : disabledAccounts) {
//            try {
//                if (account.isEnable()) {
//                    // 账号已启用，从禁用队列移除
//                    disabledAccountQueue.removeDisabledAccount(account.getChannelId());
//                    log.info("账号 {} 已启用，从禁用队列移除", account.getChannelId());
//                } else {
//                    //有账号被禁用，执行初始化器
//                    singlediscordAccountInitializer.fun();
////                    discordAccountInitializer.run(null);
//                    log.info("账号 {} 被禁用，执行初始化", account.getChannelId());
//                }
//            } catch (Exception e) {
//                log.error("处理账号 {} 时发生错误{}", account.getChannelId(), e.getMessage());
//            }
//        }
//    }
    }
}