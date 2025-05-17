package com.github.novicezk.midjourney.controller;

import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.dto.AccountDTO;
import com.github.novicezk.midjourney.dto.AccountRespondDTO;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.result.Result;
import com.github.novicezk.midjourney.util.AccountsRepeatUtils;
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

    private final AccountsRepeatUtils accountsRepeatUtils;

    private final DiscordLoadBalancer loadBalancer;

    @ApiOperation(value = "修改账号")
    @PostMapping ("/accounts")
    public Result<String> updateAccounts(@RequestBody List<AccountDTO.Account> list) {
        //修改本地的yml的accounts
//      accountsUpdateUtils.updateConfig(list);
      //发送新的accounts到nacos
        accountsUpdateUtils.sendAccountsTONacos(list);
        accountsRepeatUtils.fun(false,null);
      log.info("账号更新为："+list.toString());
      return Result.ok("账号更新成功");
    }

    @ApiOperation(value = "获取账号")
    @GetMapping ("/getInfo")
    public Map<String,Object> getInfo() {
       List<AccountRespondDTO> listmap= new ArrayList<>();
        List<DiscordAccount> list =this.loadBalancer.getAllInstances().stream().map(DiscordInstance::account).toList();
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
        Map<String,Object> map =new HashMap<>();
        map.put("total",listmap.size());
        map.put("row",listmap);
        map.put("code",200);
        map.put("msg","查询成功");
        return map;
    }

    @ApiOperation(value = "删除账号")
    @GetMapping("/delete")
    public Result<String> deleteByGuiId(@RequestParam String guildId) throws JsonProcessingException, NacosException {
        if(!guildId.isEmpty()){
            accountsUpdateUtils.deleteByGuiId(guildId);
            accountsRepeatUtils.fun(true,guildId);
            return Result.ok("删除账号:"+guildId);
        }
        return Result.fail("输入正确的guildId");
    }

    @ApiOperation(value = "添加账号")
    @PostMapping("/add")
    public Result<String> addAccount(@RequestBody AccountDTO.Account account) throws JsonProcessingException, NacosException {
      try{
          accountsUpdateUtils.addAccount(account);
          boolean enable= accountsRepeatUtils.fun(false,null);
          if(enable){
              return Result.ok("添加账号成功");
          }else {
              //把已经添加到本地的删除
              accountsUpdateUtils.deleteByGuiId(account.getGuildId());
              Result.fail("添加失败：该账号不可用");
          }
      }catch (Exception e){
          accountsUpdateUtils.deleteByGuiId(account.getGuildId());
        e.printStackTrace();
      }
        return Result.fail("添加失败：该账号不可用");
    }
}
