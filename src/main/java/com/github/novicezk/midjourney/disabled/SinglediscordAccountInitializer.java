package com.github.novicezk.midjourney.disabled;

import cn.hutool.core.exceptions.ValidateException;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.support.DiscordAccountHelper;
import com.github.novicezk.midjourney.util.AccountTimeTracker;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SinglediscordAccountInitializer {
    private final DiscordLoadBalancer discordLoadBalancer;
    private final DiscordAccountHelper discordAccountHelper;
    private final ProxyProperties properties;

    public boolean fun() {
        List<DiscordInstance> instances = this.discordLoadBalancer.getAllInstances();
        // 创建副本
        if (instances.isEmpty()) {
            log.error("重连失败：instances实例列表为空");
            return false;
        }
        for (DiscordInstance instance : instances) {
            if (!instance.account().isEnable()) {
                try {
                    instance.tryStart(true);
                    AsyncLockUtils.LockObject lock = AsyncLockUtils.waitForLock("wss:" + instance.account().getChannelId(), Duration.ofSeconds(10));
                    if (ReturnCode.SUCCESS != lock.getProperty("code", Integer.class, 0)) {
                        throw new ValidateException(lock.getProperty("description", String.class));
                    }
                    AccountTimeTracker.recordAccountEnabled(instance.account().getChannelId());
                    instance.account().setEnable(true);
                    log.info("重连成功：Account({}) reconnect success", instance.account().getDisplay());
                } catch (Exception e) {
                    log.error("重连失败：Account({}) reconnect fail, disabled: {}", instance.account().getDisplay(), e.getMessage());
                    instance.account().setEnable(false);
                }
            }
        }
        Set<String> enableInstanceIds = instances.stream().filter(DiscordInstance::isAlive).map(DiscordInstance::getInstanceId).collect(Collectors.toSet());
        log.info("重连完成：当前可用账号数 [{}] - {}", enableInstanceIds.size(), String.join(", ", enableInstanceIds));
        return true;
    }
}


