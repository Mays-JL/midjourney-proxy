package com.github.novicezk.midjourney.service;

import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component("UpdateNacosConfigManager") // 修改Bean名称
@Slf4j
public class NacosConfigManager {
    private ConfigService configService;

    public NacosConfigManager() throws NacosException {
        configService = ConfigFactory.createConfigService("localhost:8848");
        log.info("[NacosConfigManager] 初始化ConfigService, serverAddr=localhost:8848");
    }

    public boolean updateConfig(String dataId, String group, String content,String type) throws NacosException {
//        configService.publishConfig(dataId, group, content);
        try {
            boolean isPublishOk = configService.publishConfig(dataId, group, content, type);
            log.info("[updateConfig] nacos账号配置发布{} 结果: {}", content,isPublishOk);
            return isPublishOk;
        } catch (NacosException e) {
            log.error("[updateConfig] 配置发布异常, dataId={}, group={}, type={}, message={}", dataId, group, type, e.getMessage(), e);
            return false;
        }
    }

    public String getConfig(String dateId, String group, long time) {
        try {
            String content = configService.getConfig(dateId, group, time);
            log.info("[getConfig] 获取nacos账号配置信息成功, dataId={}, group={}, content={}", dateId, group, content);
            return content;
        } catch (NacosException e) {
            log.error("[getConfig] 获取nacos账号配置信息异常, dataId={}, group={}, message={}", dateId, group, e.getMessage(), e);
        }
        log.warn("[getConfig] 获取nacos账号配置信息失败, dataId={}, group={}", dateId, group);
        return "获取信息失败";
    }
}