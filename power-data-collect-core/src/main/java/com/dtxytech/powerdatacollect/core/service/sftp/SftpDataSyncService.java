package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.config.SftpProperties;
import com.dtxytech.powerdatacollect.core.service.power.PowerForecastDataService;
import com.google.common.collect.Lists;
import com.jcraft.jsch.ChannelSftp;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author zay
 * @Date 2025/12/17 16:28
 */
@Slf4j
@Service
@AllArgsConstructor
public class SftpDataSyncService {

    private static final int BATCH_SIZE = 1000;

    private final SftpConnectionManager sftpConnectionManager;
    private final SftpProperties sftpProperties;
    private final SftpDownloader sftpRecursiveDownloader;
    private final SftpFileParser sftpFileParser;
    private final PowerForecastDataService powerForecastDataService;

    public void syncFileList(IndicatorTypeEnum indicatorType) {
        ChannelSftp sftp = sftpConnectionManager.getCurrentSftp();
        try {
            List<String> pathList = sftpRecursiveDownloader.getAllFilePath(indicatorType, sftp, sftpProperties.getRemoteDir());
            List<List<String>> batchList = Lists.partition(pathList, BATCH_SIZE);
            for (List<String> batch : batchList) {
                processPathList(batch);
            }
        } catch (Exception e) {
            log.error("SftpDataSyncService syncFileList , indicatorType: {}, sftp: {}, remoteDir: {}", indicatorType, sftp, sftpProperties.getRemoteDir(), e);
        }
    }

    private void processPathList(List<String> batch) {
        for (String path : batch) {
            log.info("SftpDataSyncService processPathList, path:{}", path);
            List<PowerForecastData> list = sftpFileParser.parseFile(path);
            powerForecastDataService.saveList(list);
        }
    }
}
