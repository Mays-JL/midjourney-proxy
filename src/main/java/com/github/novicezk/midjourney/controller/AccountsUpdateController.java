package com.github.novicezk.midjourney.controller;

import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.novicezk.midjourney.disabled.SinglediscordAccountInitializer;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.dto.AccountDTO;
import com.github.novicezk.midjourney.dto.AccountRespondDTO;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.result.Result;
import com.github.novicezk.midjourney.support.DiscordAccountInitializer;
import com.github.novicezk.midjourney.util.AccountsUpdateUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Api(tags = "动态更改账号")
@RestController
@Slf4j
@RequestMapping("/update")
@RequiredArgsConstructor
public class AccountsUpdateController {

    private final AccountsUpdateUtils accountsUpdateUtils;

    private final DiscordLoadBalancer loadBalancer;
    private final DiscordAccountInitializer discordAccountInitializer;
    private final SinglediscordAccountInitializer singlediscordAccountInitializer;


    @ApiOperation(value = "获取账号")
    @GetMapping("/getInfo")
    public Map<String, Object> getInfo() {
        List<AccountRespondDTO> listmap = new ArrayList<>();
        List<DiscordAccount> list = this.loadBalancer.getAllInstances().stream().map(DiscordInstance::account).toList();
        for (DiscordAccount discordAccount : list) {
            AccountRespondDTO account = new AccountRespondDTO();
            account.setChannelId(discordAccount.getChannelId());
            account.setCoreSize(discordAccount.getCoreSize());
            account.setGuildId(discordAccount.getGuildId());
            account.setUserToken(discordAccount.getUserToken());
            account.setQueueSize(discordAccount.getQueueSize());
            account.setId(discordAccount.getId());
            account.setEnable(discordAccount.isEnable());
            account.setTimeoutMinutes(discordAccount.getTimeoutMinutes());
            listmap.add(account);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("total", listmap.size());
        map.put("row", listmap);
        map.put("code", 200);
        map.put("msg", "查询成功");
        log.info("[getInfo] 查询成功, 总数: {}", listmap.size());
        return map;
    }

    @ApiOperation(value = "删除账号")
    @GetMapping("/delete")
    public Result<String> deleteByGuiId(@RequestParam("guildId") String guildId) throws JsonProcessingException, NacosException {
        if (!guildId.isEmpty()) {
            String result = accountsUpdateUtils.deleteByGuiId(guildId);
            if (result.equals("删除账号成功")) {
                log.info("[deleteByGuiId] 删除账号成功: guildId={}", guildId);
                return Result.ok("删除账号:" + guildId);
            }else if(result.equals("未找到该账号")){
                log.error("[deleteByGuiId] 删除账号失败: guildId={} nacos发布{}", guildId,result);
                return Result.fail("删除账号失败,未找到该账号");
            }
                else {
                log.error("[deleteByGuiId] 删除账号失败: guildId={} nacos发布{}", guildId,result);
                return Result.fail("删除账号失败");
            }
        }
        log.error("[deleteByGuiId] guildId 为空，删除失败");
        return Result.fail("输入正确的guildId");
    }

    @ApiOperation(value = "添加账号")
    @PostMapping("/add")
    public Result<String> addAccount(@RequestBody AccountDTO.Account account) throws JsonProcessingException, NacosException {
        if (account.getGuildId() == null) {
            log.error("[addAccount] 添加失败：guildId 为空, account={}", account);
            return Result.fail("添加失败：该账号不能为空");
        }
        try {
            //TODO 校验账号是否 OK
            //如果不 OK，返回失败

            //如果 OK，则添加账号
            boolean res = accountsUpdateUtils.addAccount(account);
            boolean result = discordAccountInitializer.isAccountFailed(account.getChannelId());
            if (res && !result) {
                log.info("[addAccount] 添加账号成功: {}", account);
                return Result.ok("添加成功");
            }
            if (res && result) {
                log.info("[addAccount] 添加：该账号初始化异常, account={}", account);
                return Result.ok("添加失败：该账号初始化异常");
            }

        } catch (Exception e) {
            log.error("[addAccount] 添加账号异常, account={}, message: {}", account, e.getMessage(), e);
        }
        log.error("[addAccount] 添加账号失败, account={}", account);
        return Result.fail("添加失败");
    }
    @ApiOperation(value = "重连账号")
    @GetMapping("/reconnect")
    public Result<String> reconnect() {
        return singlediscordAccountInitializer.fun() ? Result.ok("重连完成") : Result.fail("重连失败");
    }
}