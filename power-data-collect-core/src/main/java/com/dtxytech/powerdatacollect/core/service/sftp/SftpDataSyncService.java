package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.enums.StationEnum;
import com.dtxytech.powerdatacollect.core.config.SftpProperties;
import com.jcraft.jsch.ChannelSftp;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author zay
 * @Date 2025/12/17 16:28
 */
@Slf4j
@Service
@AllArgsConstructor
public class SftpDataSyncService {

    private final SftpConnectionManager sftpConnectionManager;
    private final SftpProperties sftpProperties;

    private final SftpRecursiveDownloader sftpRecursiveDownloader;

    public void syncFileList(IndicatorTypeEnum fileType) {
        ChannelSftp sftp = sftpConnectionManager.getCurrentSftp();
        for (StationEnum station : StationEnum.values()) {
            log.info("Starting SFTP sync for station: {}", station);
            try {
                sftpRecursiveDownloader.downloadAndParseAllFile(sftp, sftpProperties.getRemoteDir(), fileType);
            } catch (Exception e) {
                log.error("Process station failed station: {}", station, e);
            }
        }
    }
}
