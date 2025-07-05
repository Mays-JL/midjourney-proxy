package com.github.novicezk.midjourney.disabled;

import com.github.novicezk.midjourney.domain.DiscordAccount;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DisabledAccountQueue {
    // 使用ConcurrentHashMap存储禁用账号，key为accountId，value为账号
    private final ConcurrentHashMap<String, DiscordAccount> disabledAccounts = new ConcurrentHashMap<>();

    // 添加账号到禁用队列
    public void addDisabledAccount(String accountId,DiscordAccount discordAccount) {
        disabledAccounts.put(accountId, discordAccount);
    }

    // 从禁用队列移除账号
    public void removeDisabledAccount(String accountId) {
        disabledAccounts.remove(accountId);
    }

    // 获取所有禁用账号
    public Set<DiscordAccount> getDisabledAccount() {
        return new HashSet<>(disabledAccounts.values());
    }

    // 检查账号是否在禁用队列中
    public boolean isAccountDisabled(String accountId) {
        return disabledAccounts.containsKey(accountId);
    }
}