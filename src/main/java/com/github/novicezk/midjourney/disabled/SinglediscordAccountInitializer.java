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

    public boolean fun() {
        List<DiscordInstance> instances = this.discordLoadBalancer.getAllInstances();
        // 创建副本
        if (instances.isEmpty()) {
            log.error("重连失败：instances实例列表为空");
            return false;
        }
        log.info("重连前instances账号：{}",instances.stream().map(DiscordInstance::account).toList());
        for (DiscordInstance instance : instances) {
            if (!instance.account().isEnable()) {
                try {
                    instance.tryReconnect();
//                    AsyncLockUtils.LockObject lock = AsyncLockUtils.waitForLock("wss:" + instance.account().getChannelId(), Duration.ofSeconds(30));
//                    if (ReturnCode.SUCCESS != lock.getProperty("code", Integer.class, 0)) {
//                        throw new ValidateException(lock.getProperty("description", String.class));
//                    }
                    instance.account().setEnable(true);
                    log.info("重连成功：Account({}) reconnect success", instance.account().getDisplay());
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("重连失败：Account({}) reconnect fail, disabled: {}", instance.account().getDisplay(), e.getMessage());
                    instance.account().setEnable(false);
                    return false;
                }
            }
        }
        Set<String> enableInstanceIds = instances.stream().filter(DiscordInstance::isAlive).map(DiscordInstance::getInstanceId).collect(Collectors.toSet());
        log.info("重连完成：当前可用账号数 [{}] - {}", enableInstanceIds.size(), String.join(", ", enableInstanceIds));
        log.info("重连后instances账号：{}",instances.stream().map(DiscordInstance::account).toList());
        return true;
    }
}


