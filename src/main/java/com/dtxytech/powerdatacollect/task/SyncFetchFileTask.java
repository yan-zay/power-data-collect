package com.dtxytech.powerdatacollect.task;

import com.dtxytech.powerdatacollect.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.service.SftpSyncService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;
/**
 * @Author zay
 * @Date 2025/12/13 15:13
 */
@Slf4j
@Component
@AllArgsConstructor
public class SyncFetchFileTask {

    private final SftpSyncService syncService;

    // 每10分钟拉一次短期数据
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void syncShortTermFiles() {
        log.info("Starting short-term SFTP sync...");
        Predicate<String> matcher = name -> name.endsWith("_DQ.WPD");
        syncService.syncFileList(IndicatorTypeEnum.DQ, matcher);
        log.info("Short-term sync completed.");
    }

    // 每2分钟拉一次超短期数据
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void syncUltraShortTermFiles() {
        log.info("Starting ultra-short-term SFTP sync...");
        Predicate<String> matcher = name -> name.endsWith("_CDQ.WPD");
        syncService.syncFileList(IndicatorTypeEnum.CDQ, matcher);
        log.info("Ultra-short-term sync completed.");
    }
}
