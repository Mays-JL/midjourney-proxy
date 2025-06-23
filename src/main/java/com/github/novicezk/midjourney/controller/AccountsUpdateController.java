package com.github.novicezk.midjourney.controller;

import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.core.JsonProcessingException;
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
        return map;
    }

    @ApiOperation(value = "删除账号")
    @GetMapping("/delete")
    public Result<String> deleteByGuiId(@RequestParam String guildId) throws JsonProcessingException, NacosException {
        if (!guildId.isEmpty()) {
            boolean result = accountsUpdateUtils.deleteByGuiId(guildId);
            if (result) {
                return Result.ok("删除账号:" + guildId);
            } else {
                return Result.fail("删除账号失败");
            }
        }
        return Result.fail("输入正确的guildId");
    }

    @ApiOperation(value = "添加账号")
    @PostMapping("/add")
    public Result<String> addAccount(@RequestBody AccountDTO.Account account) throws JsonProcessingException, NacosException {
        if (account.getGuildId() == null) {
            return Result.fail("添加失败：该账号不能为空");
        }
        try {
            //TODO 校验账号是否 OK
            //如果不 OK，返回失败

            //如果 OK，则添加账号
            accountsUpdateUtils.addAccount(account);
            boolean result = discordAccountInitializer.isAccountFailed(account.getChannelId());
            if(result){
                return Result.fail("添加失败：该账号初始化异常");
            }
            return Result.ok("添加成功");
        } catch (Exception e) {
            log.error("add account failed, message: {}", e.getMessage());
        }
        return Result.fail("添加失败");
    }

}