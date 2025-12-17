package com.dtxytech.powerdatacollect.service;

import com.dtxytech.powerdatacollect.config.SftpProperties;
import com.dtxytech.powerdatacollect.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.enums.StationEnum;
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
public class Qqq {

    private final SftpConnectionManager sftpConnectionManager;
    private final SftpProperties sftpProperties;

    private final SftpRecursiveDownloader sftpRecursiveDownloader;

    public void syncFileList(IndicatorTypeEnum fileType) {
        ChannelSftp sftp = sftpConnectionManager.getCurrentSftp();
        for (StationEnum station : StationEnum.values()) {
            log.info("Starting SFTP sync for station: {}", station);
            try {
                sftpRecursiveDownloader.downloadAndParseAllFiles(sftp, sftpProperties.getRemoteDir(), fileType);
//                processStation(fileType, station);
            } catch (Exception e) {
                log.error("Process station failed qqqqqqq: {}", station, e);
            }
        }
    }
}
