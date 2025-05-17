package com.github.novicezk.midjourney.dto;

import lombok.Data;


@Data
public class AccountRespondDTO {
        private String id;
        private String guildId;
        private String channelId;
        private String userToken;
        private String userAgent;
        private int coreSize;
        private int queueSize;
        private int timeoutMinutes;
        private boolean enable;
}
