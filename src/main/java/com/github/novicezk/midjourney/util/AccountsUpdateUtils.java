package com.github.novicezk.midjourney.util;


import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.dto.AccountDTO;
import com.github.novicezk.midjourney.service.NacosConfigManager;
import com.github.novicezk.midjourney.support.DiscordAccountInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
@Slf4j
@Component
//@RequiredArgsConstructor
public class AccountsUpdateUtils {
    @Value("${spring.cloud.nacos.discovery.server-addr}")
    private String serverAddr;
    @Value("midjourney-proxy-dev.yaml")
    private String dataId;
    @Value("DEFAULT_GROUP")
    private String group;
    @Value("yaml")
    private String type;
    private final ProxyProperties properties;
    private final DiscordAccountInitializer discordAccountInitializer;
    private final NacosConfigManager nacosConfigManager;


    public AccountsUpdateUtils(ProxyProperties properties, DiscordAccountInitializer discordAccountInitializer, NacosConfigManager nacosConfigManager) {
        this.properties = properties;
        this.discordAccountInitializer = discordAccountInitializer;
        this.nacosConfigManager = nacosConfigManager;
    }

    @PostConstruct
    public void init() throws NacosException {
        log.info("[init] 初始化Nacos监听, serverAddr={}, dataId={}, group={}", serverAddr, dataId, group);
        Properties nacosProperties = new Properties();
        nacosProperties.put("serverAddr", serverAddr);
        ConfigService configService = NacosFactory.createConfigService(nacosProperties);
        configService.addListener(dataId, group, new Listener() {
            @Override
            public Executor getExecutor() {
                try {

                } catch (Exception e) {
                    log.error("[Listener.getExecutor] 异常: {}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                try {

                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    JsonNode root = mapper.readTree(configInfo);
                    JsonNode accountsNode = root.path("mj").path("accounts");

                    List<ProxyProperties.DiscordAccountConfig> accounts = mapper.readerForListOf(ProxyProperties.DiscordAccountConfig.class).readValue(accountsNode);
                    properties.setAccounts(accounts);
                    discordAccountInitializer.run(null);
                } catch (Exception e) {
                    log.error("[Listener.receiveConfigInfo] 处理账号配置变更异常: {}", e.getMessage(), e);
                }
            }
        });
    }

    public boolean sendAccountsTONacos(List<AccountDTO.Account> list) {
        try {
            // 构造顶层对象
            Map<String, Object> root = new HashMap<>();
            Map<String, Object> mjMap = new HashMap<>();
            mjMap.put("accounts", list);
            root.put("mj", mjMap);

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            String yamlContent = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);

            boolean result = nacosConfigManager.updateConfig(dataId, group, yamlContent, type);
            if (result) {
                log.info("[sendAccountsTONacos] Nacos发布成功");
                return true;
            } else {
                log.error("[sendAccountsTONacos] Nacos发布失败, result={}", result);
                return false;
            }
        } catch (NacosException | JsonProcessingException e) {
            log.error("[sendAccountsTONacos] 发布账号到Nacos异常: {}", e.getMessage(), e);
            return false;
        }
    }

    public String deleteByGuiId(String guildId) {
        try {
            String content = nacosConfigManager.getConfig(dataId, group, 5000);
            if (content == null || content.trim().isEmpty()) {
                log.error("[deleteByGuiId] nacos 配置内容为空");
                return "nacos 配置内容为空";
            }
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            JsonNode root = mapper.readTree(content);
            JsonNode accountsNode = root.path("mj").path("accounts");
            List<AccountDTO.Account> accounts = new ArrayList<>();
            if (accountsNode.isArray()) {
                for (JsonNode node : accountsNode) {
                    AccountDTO.Account acc = mapper.treeToValue(node, AccountDTO.Account.class);
                    accounts.add(acc);
                }
            }
            int before = accounts.size();
            accounts = accounts.stream()
                    .filter(account -> !guildId.equals(account.getGuildId()))
                    .collect(Collectors.toList());
            if (accounts.size() == before) {
                log.info("[deleteByGuiId] 未找到 guildId={}", guildId);
                return "未找到该账号";
            }
            boolean result = sendAccountsTONacos(accounts);
            if (result) {
                log.info("[deleteByGuiId] 删除账号成功, guildId={}", guildId);
                return "删除账号成功";
            }
        } catch (Exception e) {
            log.error("[deleteByGuiId] 解析失败: {}", e.getMessage(), e);
        }
        log.error("[deleteByGuiId] 删除账号失败, guildId={}", guildId);
        return "删除账号失败";
    }

    public boolean addAccount(AccountDTO.Account account) {
        try {
            String content = nacosConfigManager.getConfig(dataId, group, 5000);
            List<AccountDTO.Account> accounts = new ArrayList<>();
            if (content != null && !content.trim().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                JsonNode root = mapper.readTree(content);
                JsonNode accountsNode = root.path("mj").path("accounts");
                if (accountsNode.isArray()) {
                    for (JsonNode node : accountsNode) {
                        AccountDTO.Account acc = mapper.treeToValue(node, AccountDTO.Account.class);
                        accounts.add(acc);
                    }
                }
            }
            accounts.add(account);
            boolean result = sendAccountsTONacos(accounts);
            if (result) {
                log.info("[addAccount] 添加账号成功: {}", account);
                return true;
            }
        } catch (Exception e) {
            log.error("[addAccount] 解析失败: {}", e.getMessage(), e);
        }
        log.error("[addAccount] 添加账号失败: {}", account);
        return false;
    }
}