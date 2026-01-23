package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.config.SftpProperties;
import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.service.power.PowerForecastDataService;
import com.dtxytech.powerdatacollect.core.task.SyncFetchFileTask;
import com.google.common.collect.Lists;
import com.jcraft.jsch.ChannelSftp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

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
    private static final int SUB_BATCH_SIZE = 10; // 每个并行任务处理的文件数
    private static final int MAX_CONCURRENT_BATCHES = 5; // 最大并发批次数

    private final SftpConnectionManager sftpConnectionManager;
    private final SftpProperties sftpProperties;
    public final SftpDownloader sftpDownloader;
    public final SftpFileParser sftpFileParser;
    private final PowerForecastDataService powerForecastDataService;
    
    private final @Qualifier("dataSyncExecutor") ThreadPoolExecutor dataSyncExecutor;

    public void syncFileList(IndicatorTypeEnum indicatorType) {
        log.info("SftpDataSyncService syncFileList start");
        ChannelSftp sftp = sftpConnectionManager.getCurrentSftp();
        log.info("SftpDataSyncService syncFileList sftp:{}", sftp);
        try {
            List<String> pathList = sftpDownloader.getAllFilePath(indicatorType, sftp, sftpProperties.getRemoteDir());
            log.info("SftpDataSyncService syncFileList, indicatorType:{}, pathList.size:{}", indicatorType, pathList.size());
            List<List<String>> batchList = Lists.partition(pathList, BATCH_SIZE);
            for (List<String> batch : batchList) {
                processPathList(indicatorType, sftp, batch);
            }
        } catch (Exception e) {
            log.error("SftpDataSyncService syncFileList , indicatorType: {}, sftp: {}, remoteDir: {}", indicatorType, sftp, sftpProperties.getRemoteDir(), e);
        }
    }

    private void processPathList(IndicatorTypeEnum indicatorType, ChannelSftp sftp, List<String> batch) {
        // 判断是否为初始化状态，如果是则使用并行处理
        if (SyncFetchFileTask.INITIALIZED) {
            // 日常任务，使用串行处理
            for (String path : batch) {
                log.info("SftpDataSyncService processPathList, path:{}", path);
                List<PowerForecastData> list = sftpFileParser.parseFile(indicatorType, sftp, path);
                powerForecastDataService.saveList(list);
            }
        } else {
            // 初始化阶段，使用分段并行处理，每个线程创建独立的SFTP连接
            processPathListSegmentedInParallel(indicatorType, batch);
        }
    }

    /**
     * 分段并行处理路径列表 - 用于初始化阶段，每个线程创建独立的SFTP连接
     */
    private void processPathListSegmentedInParallel(IndicatorTypeEnum indicatorType, List<String> batch) {
        log.info("SftpDataSyncService processPathListSegmentedInParallel, batch size: {}", batch.size());
        
        // 将批次按并发数分组，限制同时执行的并行任务数量
        List<List<String>> subBatches = Lists.partition(batch, SUB_BATCH_SIZE);
        List<List<List<String>>> groupedSubBatches = Lists.partition(subBatches, MAX_CONCURRENT_BATCHES);
        
        for (List<List<String>> group : groupedSubBatches) {
            log.info("Processing group of {} sub-batches", group.size());
            
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (List<String> subBatch : group) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    // 每个线程创建自己的SFTP连接
                    ChannelSftp localSftp = sftpConnectionManager.createNewSftpConnection();
                    try {
                        for (String path : subBatch) {
                            try {
                                log.debug("Processing file in parallel: {}", path);
                                List<PowerForecastData> list = sftpFileParser.parseFile(indicatorType, localSftp, path);
                                if (list != null && !list.isEmpty()) {
                                    powerForecastDataService.saveList(list);
                                }
                            } catch (Exception e) {
                                log.error("Error processing file: {} - {}", path, e.getMessage(), e);
                            }
                        }
                    } finally {
                        // 任务完成后释放连接
                        SftpConnectionManager.releaseParallelConnection(Thread.currentThread().getId());
                    }
                }, dataSyncExecutor);
                futures.add(future);
            }

            // 等待当前组的所有并行任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.info("SftpDataSyncService Completed group of {} sub-batches", group.size());
        }
        log.info("SftpDataSyncService processPathListSegmentedInParallel completed");
    }
}
