package com.dtxytech.powerdatacollect.task;

import com.dtxytech.powerdatacollect.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.service.Qqq;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
/**
 * @Author zay
 * @Date 2025/12/13 15:13
 */
@Slf4j
@Component
@AllArgsConstructor
public class SyncFetchFileTask {

    private final Qqq qqq;

    // 每10分钟拉一次短期数据
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void syncShortTermFiles() {
        log.info("Starting short-term SFTP sync...");
        qqq.syncFileList(IndicatorTypeEnum.DQ);
        log.info("Short-term sync completed.");
    }

    // 每2分钟拉一次超短期数据
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void syncUltraShortTermFiles() {
        log.info("Starting ultra-short-term SFTP sync...");
        qqq.syncFileList(IndicatorTypeEnum.CDQ);
        log.info("Ultra-short-term sync completed.");
    }
}
