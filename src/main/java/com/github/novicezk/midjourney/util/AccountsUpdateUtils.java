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
        Properties nacosProperties = new Properties();
        nacosProperties.put("serverAddr", serverAddr);
        ConfigService configService = NacosFactory.createConfigService(nacosProperties);
        configService.addListener(dataId, group, new Listener() {
            @Override
            public Executor getExecutor() {
                try {

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                try {
//                    configInfo = configInfo.substring(19);
//                    ObjectMapper mapper = new ObjectMapper();
//                    List<ProxyProperties.DiscordAccountConfig> accounts = mapper.readValue(configInfo, new TypeReference<>() {});

                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    JsonNode root = mapper.readTree(configInfo);
                    JsonNode accountsNode = root.path("mj").path("accounts");

                    List<ProxyProperties.DiscordAccountConfig> accounts = mapper.readerForListOf(ProxyProperties.DiscordAccountConfig.class).readValue(accountsNode);
                    properties.setAccounts(accounts);
                    log.info("{} 本次修改后的内容: {}", dataId, configInfo);
                    discordAccountInitializer.run(null);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(e.getMessage());
                }
            }
        });
    }

    public boolean sendAccountsTONacos(List<AccountDTO.Account> list) {
        try {
//            NacosConfigManager nacosConfigManager =new NacosConfigManager();
            // 构造顶层对象
            Map<String, Object> root = new HashMap<>();
            Map<String, Object> mjMap = new HashMap<>();
            mjMap.put("accounts", list);
            root.put("mj", mjMap);

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            String yamlContent = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);

            boolean result = nacosConfigManager.updateConfig(dataId, group, yamlContent, type);
            if(result){
                return true;
            }else {
                log.error("nacos发布失败结果为{}",result);
                return  false;
            }
        } catch (NacosException | JsonProcessingException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    public boolean deleteByGuiId(String guiId) throws NacosException, JsonProcessingException {
//        NacosConfigManager nacosConfigManager = new NacosConfigManager();
        String content = nacosConfigManager.getConfig(dataId, group, 5000);
        if (content == null) {
            return false;
        }
        ObjectMapper mapper = new YAMLMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        AccountDTO accountDTO = mapper.readValue(content, AccountDTO.class);
        accountDTO.getMj().setAccounts(accountDTO.getMj().getAccounts().stream()
                .filter(account -> !account.getGuildId().equals(guiId))
                .collect(Collectors.toList()));
        boolean result = sendAccountsTONacos(accountDTO.getMj().getAccounts());
       if(result){
           return true;
       }
       return false;
    }

    public boolean addAccount(AccountDTO.Account account) throws NacosException, JsonProcessingException {
        //TODO 不应每次创建每次创建 nacosConfigManager
//        NacosConfigManager nacosConfigManager = new NacosConfigManager();
        String content = nacosConfigManager.getConfig(dataId, group, 5000);
        ObjectMapper mapper = new YAMLMapper();
        //忽略未知属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        AccountDTO accountDTO;
        if (content == null) {
            accountDTO = new AccountDTO();
            accountDTO.getMj().setAccounts(new ArrayList<>());
        } else {
            accountDTO = mapper.readValue(content, AccountDTO.class);
        }
        if (accountDTO.getMj().getAccounts() == null) {
            accountDTO.getMj().setAccounts(new ArrayList<>());
        }
        accountDTO.getMj().getAccounts().add(account);
        boolean result = sendAccountsTONacos(accountDTO.getMj().getAccounts());
        if(result){
            return true;
        }
        return false;
    }
}