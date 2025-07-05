package com.github.novicezk.midjourney.disabled;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.support.DiscordAccountHelper;
import com.github.novicezk.midjourney.util.AccountTimeTracker;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SinglediscordAccountInitializer {
    private final DiscordLoadBalancer discordLoadBalancer;
    private final DiscordAccountHelper discordAccountHelper;
    private final ProxyProperties properties;
    public void fun(){
//        ProxyProperties.ProxyConfig proxy = this.properties.getProxy();
//        if (Strings.isNotBlank(proxy.getHost())) {
//            System.setProperty("http.proxyHost", proxy.getHost());
//            System.setProperty("http.proxyPort", String.valueOf(proxy.getPort()));
//            System.setProperty("https.proxyHost", proxy.getHost());
//            System.setProperty("https.proxyPort", String.valueOf(proxy.getPort()));
//        }

//        List<ProxyProperties.DiscordAccountConfig> configAccounts = this.properties.getAccounts();
//        if (CharSequenceUtil.isNotBlank(this.properties.getDiscord().getChannelId())) {
//            configAccounts.add(this.properties.getDiscord());
//        }
//        discordLoadBalancer.getAllInstances().clear();
        List<DiscordInstance> instances = this.discordLoadBalancer.getAllInstances();
        // 创建副本
//        List<ProxyProperties.DiscordAccountConfig> safeConfigAccounts;
//        synchronized (configAccounts) {
//            safeConfigAccounts = new ArrayList<>(configAccounts);
//        }
        for (DiscordInstance instance : instances) {
            if (!instance.account().isEnable()) {
                try {
//                    DiscordInstance instance = this.discordAccountHelper.createDiscordInstance(account);
//                    log.info("[SinglediscordAccountInitializer ]:账号状态{}",account.isEnable());
//                if (!account.isEnable()) {
//                    return;
//                }
                    instance.startWss();
                    AsyncLockUtils.LockObject lock = AsyncLockUtils.waitForLock("wss:" + instance.account().getChannelId(), Duration.ofSeconds(10));
                    if (ReturnCode.SUCCESS != lock.getProperty("code", Integer.class, 0)) {
                        throw new ValidateException(lock.getProperty("description", String.class));
                    }
//                    instances.add(instance);
                    AccountTimeTracker.recordAccountEnabled(instance.account().getChannelId());
                    instance.account().setEnable(true);
                } catch (Exception e) {
                    log.error("Account({}) check fail, disabled: {}", instance.account().getDisplay(), e.getMessage());
                    instance.account().setEnable(false);
                }
            }
//        for (ProxyProperties.DiscordAccountConfig configAccount : safeConfigAccounts) {
//            DiscordAccount account = new DiscordAccount();
        }
//            BeanUtil.copyProperties(SingleAccount, account);
//            account.setId(SingleAccount.getChannelId());

        Set<String> enableInstanceIds = instances.stream().filter(DiscordInstance::isAlive).map(DiscordInstance::getInstanceId).collect(Collectors.toSet());
        log.info("当前可用账号数 [{}] - {}", enableInstanceIds.size(), String.join(", ", enableInstanceIds));
        }

    }


