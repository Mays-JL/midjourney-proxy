package com.github.novicezk.midjourney.service;

import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.support.DiscordAccountInitializer;
import com.github.novicezk.midjourney.util.AccountsUpdateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AccountDisableManager {
    
    private final Map<String, DiscordAccount> disabledAccounts = new ConcurrentHashMap<>();
    private final DiscordAccountInitializer discordAccountInitializer;
    private final AccountsUpdateUtils accountsUpdateUtils;
    
    @Autowired
    public AccountDisableManager(DiscordAccountInitializer discordAccountInitializer, 
                               AccountsUpdateUtils accountsUpdateUtils) {
        this.discordAccountInitializer = discordAccountInitializer;
        this.accountsUpdateUtils = accountsUpdateUtils;
    }
    
    /**
     * 添加账号到禁用队列
     */
    public void addDisabledAccount(DiscordAccount account) {
        if (account != null && account.getId() != null) {
            disabledAccounts.put(account.getId(), account);
            log.info("[AccountDisableManager] 账号已添加到禁用队列: {}", account.getDisplay());
        }
    }
    
    /**
     * 从禁用队列中移除账号
     */
    public void removeDisabledAccount(String accountId) {
        DiscordAccount removed = disabledAccounts.remove(accountId);
        if (removed != null) {
            log.info("[AccountDisableManager] 账号已从禁用队列移除: {}", removed.getDisplay());
        }
    }
    
    /**
     * 获取禁用队列大小
     */
    public int getDisabledAccountsCount() {
        return disabledAccounts.size();
    }
    
    /**
     * 获取禁用队列中的所有账号
     */
    public Map<String, DiscordAccount> getDisabledAccounts() {
        return new ConcurrentHashMap<>(disabledAccounts);
    }
    
    /**
     * 定时任务：每5分钟检查一次被禁用的账号
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void checkDisabledAccounts() {
        if (disabledAccounts.isEmpty()) {
            return;
        }
        
        log.info("[AccountDisableManager] 开始检查禁用队列中的账号，当前队列大小: {}", disabledAccounts.size());
        
        // 先运行DiscordAccountInitializer重新加载最新的账号状态
        try {
            log.info("[AccountDisableManager] 重新加载账号状态...");
            discordAccountInitializer.run(null);
            log.info("[AccountDisableManager] 账号状态重新加载完成");
        } catch (Exception e) {
            log.error("[AccountDisableManager] 重新加载账号状态失败: {}", e.getMessage(), e);
        }
        
        // 创建当前队列的快照，避免并发修改
        Map<String, DiscordAccount> snapshot = new ConcurrentHashMap<>(disabledAccounts);
        
        for (Map.Entry<String, DiscordAccount> entry : snapshot.entrySet()) {
            String accountId = entry.getKey();
            DiscordAccount account = entry.getValue();
            
            try {
                // 检查账号是否已启用（重新加载后的状态）
                if (Boolean.TRUE.equals(account.isEnable())) {
                    log.info("[AccountDisableManager] 账号已启用，从禁用队列移除: {}", account.getDisplay());
                    removeDisabledAccount(accountId);
                } else {
                    log.debug("[AccountDisableManager] 账号仍处于禁用状态: {}", account.getDisplay());
                }
            } catch (Exception e) {
                log.error("[AccountDisableManager] 检查账号状态时发生异常: {}, 账号: {}", e.getMessage(), account.getDisplay(), e);
            }
        }
        
        // 如果队列不为空，说明还有被禁用的账号，再次运行初始化器确保状态同步
        if (!disabledAccounts.isEmpty()) {
            log.info("[AccountDisableManager] 检测到禁用账号，再次运行DiscordAccountInitializer确保状态同步");
            try {
                discordAccountInitializer.run(null);
                log.info("[AccountDisableManager] DiscordAccountInitializer执行完成");
            } catch (Exception e) {
                log.error("[AccountDisableManager] 运行DiscordAccountInitializer时发生异常: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 手动触发检查（用于测试或紧急情况）
     */
    public void manualCheck() {
        log.info("[AccountDisableManager] 手动触发检查禁用账号");
        checkDisabledAccounts();
    }
} 