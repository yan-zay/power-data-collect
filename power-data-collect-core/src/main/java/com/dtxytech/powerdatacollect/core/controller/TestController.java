package com.dtxytech.powerdatacollect.core.controller;

import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.service.sftp.SftpDataSyncService;
import com.dtxytech.powerdatacollect.core.service.test.TestService;
import com.dtxytech.powerdatacollect.core.task.SyncFetchFileTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @Author zay
 * @Date 2025/11/21 9:34
 */
@Slf4j
@RestController
@RequestMapping("/test2")
@AllArgsConstructor
public class TestController {

    @Resource(name = "queryBusinessExecutor")
    private Executor asyncExecutor;

    private final TestService testService;
    private final SftpDataSyncService sftpDataSyncService;

    @GetMapping(value = "/test01")
    public String test01() {
        return "test01";
    }

    @PostMapping(value = "/insertData")
    public String insertData(@RequestBody Map<String, String> dto) {
        return testService.insertData(dto);
    }

    @GetMapping(value = "/updateInitStatus")
    public Boolean updateInitStatus(@RequestParam Boolean status) {
        log.info("TestController updateInitStatus start, INITIALIZED:{}, status:{}", SyncFetchFileTask.INITIALIZED, status);
        SyncFetchFileTask.INITIALIZED = status;
        log.info("TestController updateInitStatus end, INITIALIZED:{}, status:{}", SyncFetchFileTask.INITIALIZED, status);
        return SyncFetchFileTask.INITIALIZED;
    }

    @GetMapping(value = "/syncFilesAsync")
    public String syncFilesAsync(@RequestParam String indicatorType) {
        try {
            IndicatorTypeEnum type;
            if (indicatorType == null || indicatorType.trim().isEmpty()) {
                type = IndicatorTypeEnum.DQ;
            } else {
                type = IndicatorTypeEnum.valueOf(indicatorType.toUpperCase());
            }

            String taskId = "task_" + System.currentTimeMillis() + "_" + type.getName();

            CompletableFuture.runAsync(() -> {
                try {
                    log.info("Async syncFiles task [{}] started, indicatorType:{}", taskId, indicatorType);
                    sftpDataSyncService.syncFileList(type);
                    log.info("Async syncFiles task [{}] completed successfully, indicatorType:{}", taskId, indicatorType);
                } catch (Exception e) {
                    log.error("Async syncFiles task [{}] failed, indicatorType:{}", taskId, indicatorType, e);
                }
            }, asyncExecutor);

            return taskId;
        } catch (IllegalArgumentException e) {
            log.error("syncFilesAsync failed, invalid indicatorType:{}", indicatorType, e);
            return "error";
        } catch (Exception e) {
            log.error("syncFilesAsync failed, indicatorType:{}", indicatorType, e);
            return "error";
        }
    }
}
