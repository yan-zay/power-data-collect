package com.dtxytech.powerdatacollect.core.service.sftp;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @Author zay
 * @Date 2026/1/12 10:28
 */
@Component
@AllArgsConstructor
@ConditionalOnProperty(name = "sftp.region", havingValue = "neimeng", matchIfMissing = false)
public class SftpDownloaderNeimeng extends SftpDownloader {


}
