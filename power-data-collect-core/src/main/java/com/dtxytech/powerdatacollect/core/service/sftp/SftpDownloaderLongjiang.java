package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.jcraft.jsch.ChannelSftp;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @Author zay
 * @Date 2026/1/12 10:28
 */
@Component
@AllArgsConstructor
@ConditionalOnProperty(name = "sftp.region", havingValue = "longjiang", matchIfMissing = false)
public class SftpDownloaderLongjiang extends SftpDownloader {

    @Override
    protected void downloadAndParseAllFile(ChannelSftp sftp, String remoteDir, IndicatorTypeEnum indicatorType) {

    }
}
