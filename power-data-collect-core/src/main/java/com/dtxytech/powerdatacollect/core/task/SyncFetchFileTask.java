package com.dtxytech.powerdatacollect.core.task;

import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.service.sftp.SftpDataSyncService;
import com.dtxytech.powerdatacollect.core.service.sftp.SftpDownloader;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;

/**
 * @Author zay
 * @Date 2025/12/13 15:13
 */
@Slf4j
@Component
@AllArgsConstructor
public class SyncFetchFileTask {

    public volatile static boolean INITIALIZED = false;
    private final SftpDataSyncService sftpDataSyncService;

    // 每10分钟拉一次短期数据
    @Scheduled(cron = "0 0 0/1 * * ? ")
    public void syncShortTermFile() {
        log.info("Starting syncShortTermFile SFTP sync");
        if (!INITIALIZED) {
            return;
        }
        sftpDataSyncService.syncFileList(IndicatorTypeEnum.DQ);
        log.info("syncShortTermFile completed");
    }

    // 每2分钟拉一次超短期数据
    @Scheduled(cron = "0 0/5 * * * ? ")
    public void syncVeryShortTermFile() {
        log.info("Starting syncVeryShortTermFile SFTP sync");
        if (!INITIALIZED) {
            return;
        }
        sftpDataSyncService.syncFileList(IndicatorTypeEnum.CDQ);
        log.info("syncVeryShortTermFile sync completed");
    }

    @PostConstruct
    public void init() {
        log.info("SyncFetchFileTask init");
        CompletableFuture.runAsync(() -> sftpDataSyncService.syncFileList(IndicatorTypeEnum.DQ));
        sftpDataSyncService.syncFileList(IndicatorTypeEnum.CDQ);
        INITIALIZED = true;
        log.info("SyncFetchFileTask init completed");
    }
}
