package com.github.novicezk.midjourney.util;


import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.dto.AccountDTO;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.service.NacosConfigManager;
import com.github.novicezk.midjourney.support.DiscordAccountInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
@Slf4j
@Component
//@RequiredArgsConstructor
public class AccountsUpdateUtils {
    @Value("${filePath}")
    private  String filePath;
    @Value("${spring.cloud.nacos.discovery.server-addr}")
    private String serverAddr;
    @Value("midjourney-proxy-dev.yaml")
    private  String dataId;
    @Value("DEFAULT_GROUP")
    private String group;
    @Value("yaml")
    private String type;
    private final ProxyProperties properties;
    private final DiscordAccountInitializer discordAccountInitializer;


    public AccountsUpdateUtils(ProxyProperties properties, DiscordAccountInitializer discordAccountInitializer) {
        this.properties = properties;
        this.discordAccountInitializer = discordAccountInitializer;
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
                    discordAccountInitializer.run(null);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        });
    }

    public void updateConfig(List<AccountDTO.Account> list) {
        Yaml yaml = new Yaml();
        Map<String, Object> obj;

        try (InputStream inputStream = new FileInputStream(filePath)) {
            obj = yaml.load(inputStream);
        } catch (IOException e) {
//            e.printStackTrace();
            log.error("update config failed, {}", e.getMessage());
            return;
        }

        // 获取 mj.accounts 配置
        List<Map<String, Object>> accounts = (List<Map<String, Object>>) ((Map<String, Object>) obj.get("mj")).get("accounts");
        if (accounts == null) {
            accounts = new ArrayList<>();
            ((Map<String, Object>) obj.get("mj")).put("accounts", accounts);
        }
        //清空当前列表
        accounts.clear();
        for (AccountDTO.Account accountDTO : list) {
            Map<String, Object> account = new LinkedHashMap<>();
            account.put("guild-id", accountDTO.getGuildId());
            account.put("channel-id", accountDTO.getChannelId());
            account.put("user-token", accountDTO.getUserToken());
            account.put("user-agent", accountDTO.getUserAgent());
            account.put("core-size", accountDTO.getCoreSize());
            account.put("queue-size", accountDTO.getQueueSize());
            accounts.add(account);
        }

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml newYaml = new Yaml(options);

        try (FileWriter writer = new FileWriter(filePath)) {
            newYaml.dump(obj, writer);
        } catch (IOException e) {
//            e.printStackTrace();
            log.error("account update failed: {}", e.getMessage());
        }
    }
    public  boolean sendAccountsTONacos(List<AccountDTO.Account> list) {
        try {

            NacosConfigManager nacosConfigManager =new NacosConfigManager();
            // 构造顶层对象
            Map<String, Object> root = new HashMap<>();
            Map<String, Object> mjMap = new HashMap<>();
            mjMap.put("accounts", list);
            root.put("mj", mjMap);

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            String yamlContent = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);

            nacosConfigManager.updateConfig(dataId, group, yamlContent,type);
            return true;
        } catch (NacosException | JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return false;
    }
    public String deleteByGuiId(String guiId) throws NacosException, JsonProcessingException {
        NacosConfigManager nacosConfigManager = new NacosConfigManager();
        String content = nacosConfigManager.getConfig(dataId, group, 5000);
        if(content == null){
            return "账号为空删除失败";
        }
        ObjectMapper mapper = new YAMLMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        AccountDTO accountDTO = mapper.readValue(content, AccountDTO.class);
        accountDTO.getMj().setAccounts(accountDTO.getMj().getAccounts().stream()
                .filter(account -> !account.getGuildId().equals(guiId))
                .collect(Collectors.toList()));
        sendAccountsTONacos(accountDTO.getMj().getAccounts());
//        updateLocalProxyProperties(accountDTO.getMj().getAccounts());
        return "删除成功";
    }
//    public List<DiscordInstance> deleteByGuiIdInInstances(List<DiscordInstance> instances,String guildId) {
//            Iterator<DiscordInstance> iterator = instances.iterator();
//            while (iterator.hasNext()) {
//                DiscordInstance instance = iterator.next();
//                DiscordAccount account = instance.account();
//                if (account != null && guildId.equals(account.getGuildId())) {
//                    iterator.remove();
//                }
//            }
//            return instances;
//    }
    public void addAccount(AccountDTO.Account account) throws NacosException, JsonProcessingException {
        //TODO 不应每次创建每次创建 nacosConfigManager
        NacosConfigManager nacosConfigManager = new NacosConfigManager();
        String content = nacosConfigManager.getConfig(dataId, group, 5000);
        ObjectMapper mapper = new YAMLMapper();
        //忽略未知属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        AccountDTO accountDTO;
        if(content==null){
            accountDTO =new AccountDTO();
            accountDTO.getMj().setAccounts(new ArrayList<>());
        }else {
             accountDTO = mapper.readValue(content, AccountDTO.class);
        }
        if(accountDTO.getMj().getAccounts()==null){
            accountDTO.getMj().setAccounts(new ArrayList<>() );
        }
        accountDTO.getMj().getAccounts().add(account);
        sendAccountsTONacos(accountDTO.getMj().getAccounts());
        //  手动更新当前 ProxyProperties
//        updateLocalProxyProperties(accountDTO.getMj().getAccounts());
    }
    private void updateLocalProxyProperties(List<AccountDTO.Account> accounts) {
        // 清空旧数据
        this.properties.getAccounts().clear();

        // 添加到当前实例
        accounts.forEach(account -> {
            ProxyProperties.DiscordAccountConfig config = new ProxyProperties.DiscordAccountConfig();
            config.setGuildId(account.getGuildId());
            config.setUserToken(account.getUserToken());
            config.setChannelId(account.getChannelId());
            config.setUserToken(account.getUserToken());
            config.setCoreSize(account.getCoreSize());
            config.setQueueSize(account.getQueueSize());
            this.properties.getAccounts().add(config);
        });
    }
}
