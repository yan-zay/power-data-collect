package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.config.SftpProperties;
import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.service.power.PowerForecastDataService;
import com.google.common.collect.Lists;
import com.jcraft.jsch.ChannelSftp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author zay
 * @Date 2025/12/17 16:28
 */
@Data
@Slf4j
@Service
@AllArgsConstructor
public class SftpDataSyncService {

    private static final int BATCH_SIZE = 1000;

    private final SftpConnectionManager sftpConnectionManager;
    private final SftpProperties sftpProperties;
    public final SftpDownloader sftpDownloader;
    public final SftpFileParser sftpFileParser;
    private final PowerForecastDataService powerForecastDataService;

    public void syncFileList(IndicatorTypeEnum indicatorType) {
        log.info("SftpDataSyncService syncFileList start");
        ChannelSftp sftp = sftpConnectionManager.getCurrentSftp();
        log.info("SftpDataSyncService syncFileList sftp:{}", sftp);
        try {
            List<String> pathList = sftpDownloader.getAllFilePath(indicatorType, sftp, sftpProperties.getRemoteDir());
            log.info("SftpDataSyncService syncFileList, indicatorType:{}, pathList.size:{}", indicatorType, pathList.size());
            List<List<String>> batchList = Lists.partition(pathList, BATCH_SIZE);
            for (List<String> batch : batchList) {
                processPathList(indicatorType, batch);
            }
        } catch (Exception e) {
            log.error("SftpDataSyncService syncFileList , indicatorType: {}, sftp: {}, remoteDir: {}", indicatorType, sftp, sftpProperties.getRemoteDir(), e);
        }
    }

    private void processPathList(IndicatorTypeEnum indicatorType, List<String> batch) {
        ChannelSftp sftp = sftpConnectionManager.getCurrentSftp();
        for (String path : batch) {
            log.info("SftpDataSyncService processPathList， indicatorType：{}, path:{}", indicatorType, path);
            if (powerForecastDataService.checkFileExists(indicatorType, path)) {
                log.info("SftpDataSyncService processPathList file exists， indicatorType：{}, path:{}", indicatorType, path);
                continue;
            }
            List<PowerForecastData> list = sftpFileParser.parseFile(indicatorType, sftp, path);
            powerForecastDataService.saveList(list);
        }
    }
}
