package com.github.novicezk.midjourney.support;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.util.AccountTimeTracker;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
public class DiscordAccountInitializer implements ApplicationRunner {
	private final DiscordLoadBalancer discordLoadBalancer;
	private final DiscordAccountHelper discordAccountHelper;
	private final ProxyProperties properties;
	private final Set<String> failedAccounts = ConcurrentHashMap.newKeySet();


	@Override
	public void run(ApplicationArguments args) throws Exception {
		ProxyProperties.ProxyConfig proxy = this.properties.getProxy();
		if (Strings.isNotBlank(proxy.getHost())) {
			System.setProperty("http.proxyHost", proxy.getHost());
			System.setProperty("http.proxyPort", String.valueOf(proxy.getPort()));
			System.setProperty("https.proxyHost", proxy.getHost());
			System.setProperty("https.proxyPort", String.valueOf(proxy.getPort()));
		}
		List<ProxyProperties.DiscordAccountConfig> configAccounts = this.properties.getAccounts();
		if (CharSequenceUtil.isNotBlank(this.properties.getDiscord().getChannelId())) {
			configAccounts.add(this.properties.getDiscord());
		}
		discordLoadBalancer.getAllInstances().clear();
		List<DiscordInstance> instances = this.discordLoadBalancer.getAllInstances();
		// 创建副本
		List<ProxyProperties.DiscordAccountConfig> safeConfigAccounts;
		synchronized (configAccounts) {
			safeConfigAccounts = new ArrayList<>(configAccounts);
		}

		for (ProxyProperties.DiscordAccountConfig configAccount : safeConfigAccounts) {
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
				AccountTimeTracker.recordAccountEnabled(instance.account().getChannelId());
			} catch (Exception e) {
				log.error("Account({}) init fail, disabled: {}", account.getDisplay(), e.getMessage());
				account.setEnable(false);
				failedAccounts.add(account.getChannelId()); // 记录失败账号
			}
		}
		Set<String> enableInstanceIds = instances.stream().filter(DiscordInstance::isAlive).map(DiscordInstance::getInstanceId).collect(Collectors.toSet());
		log.info("当前可用账号数 [{}] - {}", enableInstanceIds.size(), String.join(", ", enableInstanceIds));
	}
	public boolean isAccountFailed(String channelId) {
		return failedAccounts.contains(channelId);
	}

}
