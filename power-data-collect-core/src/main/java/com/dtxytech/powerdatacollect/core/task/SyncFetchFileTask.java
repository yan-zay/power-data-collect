package com.dtxytech.powerdatacollect.core.task;

import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.service.sftp.SftpDataSyncService;
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

    private final SftpDataSyncService sftpDataSyncService;

    // 每10分钟拉一次短期数据
//    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void syncShortTermFile() {
        log.info("Starting syncShortTermFile SFTP sync");
        sftpDataSyncService.syncFileList(IndicatorTypeEnum.DQ);
        log.info("syncShortTermFile completed");
    }

    // 每2分钟拉一次超短期数据
//    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void syncVeryShortTermFile() {
        log.info("Starting syncVeryShortTermFile SFTP sync");
        sftpDataSyncService.syncFileList(IndicatorTypeEnum.CDQ);
        log.info("syncVeryShortTermFile sync completed");
    }

    @PostConstruct
    public void init() {
        // 使用 CompletableFuture 异步执行两个方法
        CompletableFuture.runAsync(this::syncShortTermFile);
        CompletableFuture.runAsync(this::syncVeryShortTermFile);
    }
}
