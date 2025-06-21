package com.github.novicezk.midjourney.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.support.DiscordAccountHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;


import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountsRepeatUtils {
    private final DiscordLoadBalancer discordLoadBalancer;
    private final  DiscordAccountHelper discordAccountHelper;
    private final ProxyProperties properties;
    private final AccountsUpdateUtils accountsUpdateUtils;
    public boolean fun(boolean isDelete,String guildId) {
        ProxyProperties.ProxyConfig proxy = properties.getProxy();
        if (Strings.isNotBlank(proxy.getHost())) {
            System.setProperty("http.proxyHost", proxy.getHost());
            System.setProperty("http.proxyPort", String.valueOf(proxy.getPort()));
            System.setProperty("https.proxyHost", proxy.getHost());
            System.setProperty("https.proxyPort", String.valueOf(proxy.getPort()));
        }
        List<ProxyProperties.DiscordAccountConfig> configAccounts = properties.getAccounts();
        if(configAccounts.size()==0){
            return false;
        }
        if (CharSequenceUtil.isNotBlank(properties.getDiscord().getChannelId())) {
        configAccounts.add(properties.getDiscord());
    }
        List<DiscordInstance> instances = this.discordLoadBalancer.getAllInstances();

        Set<String> existingIds = instances.stream()
                .map(i -> i.account().getChannelId())
                .collect(Collectors.toSet());
        if(isDelete){
        instances= accountsUpdateUtils.deleteByGuiIdInInstances(instances,guildId);
        }
        for (ProxyProperties.DiscordAccountConfig configAccount : configAccounts) {
            if (existingIds.contains(configAccount.getChannelId())) {
                continue;
            }
            DiscordAccount account = new DiscordAccount();
            BeanUtil.copyProperties(configAccount, account);
            account.setId(configAccount.getChannelId());
            try {
                DiscordInstance instance = this.discordAccountHelper.createDiscordInstance(account);
                if (!account.isEnable()) {
                    continue;
                }
                instance.startWss();
                AsyncLockUtils.LockObject lock = AsyncLockUtils.waitForLock("wss:" + account.getChannelId(), Duration.ofSeconds(10));
                if (ReturnCode.SUCCESS != lock.getProperty("code", Integer.class, 0)) {
                    throw new ValidateException(lock.getProperty("description", String.class));
                }
                instances.add(instance);
            } catch (Exception e) {
                log.error("Account({}) init fail, disabled: {}", account.getDisplay(), e.getMessage());
                account.setEnable(false);
                return false;
            }
        }
        Set<String> enableInstanceIds = instances.stream().filter(DiscordInstance::isAlive).map(DiscordInstance::getInstanceId).collect(Collectors.toSet());
        log.info("当前可用账号数 [{}] - {}", enableInstanceIds.size(), String.join(", ", enableInstanceIds));
        return true;
    }

}
