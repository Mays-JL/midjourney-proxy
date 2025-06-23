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
    }

    public boolean updateConfig(String dataId, String group, String content,String type) throws NacosException {
//        configService.publishConfig(dataId, group, content);
        try {
            boolean isPublishOk = configService.publishConfig(dataId, group, content,type);
            log.info("配置发布结果：" + isPublishOk);
            return isPublishOk;
        } catch (NacosException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getConfig(String dateId,String group,long time){
        try {
            String content = configService.getConfig(dateId,group,time);
            return content;
        }catch (NacosException e){
            e.printStackTrace();
        }
        return "获取信息失败";
    }
}