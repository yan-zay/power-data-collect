package com.dtxytech.powerdatacollect.core.config;

/**
 * @Author zay
 * @Date 2025/12/13 15:29
 */
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sftp")
public class SftpProperties {
    private String host;
    private int port = 22;
    private String username;
    private String password;
    private String remoteDir;
    private String localDownloadDir;
    private String fileStartDate;
}
