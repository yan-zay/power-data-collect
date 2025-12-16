package com.dtxytech.powerdatacollect.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author zay
 * @Date 2025/12/13 11:32
 */
@Data
@Component
@ConfigurationProperties(prefix = "sync")
public class SyncConfig {

    private Boolean enabled = true;
    private String cron = "0 */5 * * * ?";  // 每5分钟执行一次
    private Integer maxRetry = 3;
    private Integer moveRetryCount = 3;
    private Long minFileSize = 0L;
    private Long maxFileSize = 1024L * 1024 * 1024; // 1GB
    private Integer connectTimeout = 5 * 1000;
}
