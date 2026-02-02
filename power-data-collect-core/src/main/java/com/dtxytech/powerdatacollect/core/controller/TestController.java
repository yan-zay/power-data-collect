package com.dtxytech.powerdatacollect.core.controller;

import com.dtxytech.powerdatacollect.core.service.test.TestService;
import com.dtxytech.powerdatacollect.core.task.SyncFetchFileTask;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @Author zay
 * @Date 2025/11/21 9:34
 */
@RestController
@RequestMapping("/test2")
@AllArgsConstructor
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);
    private final TestService testService;
    private final SyncFetchFileTask syncFetchFileTask;

    @PostMapping(value = "/test01")
    public String test01() {
        return "test01";
    }

    @PostMapping(value = "/test02")
    public String test02() {
        testService.test02(null);
        return "test02";
    }

    @PostMapping(value = "/insertData")
    public String insertData(@RequestBody Map<String, String> dto) {
        return testService.insertData(dto);
    }

    @GetMapping(value = "/test04")
    public String test04() {
//        syncFetchFileTask.syncVeryShortTermFile();
        return "test04";
    }

    @GetMapping(value = "/updateInitStatus")
    public Boolean updateInitStatus(@RequestParam Boolean status) {
        log.info("TestController updateInitStatus start, INITIALIZED:{}, status:{}", SyncFetchFileTask.INITIALIZED, status);
        SyncFetchFileTask.INITIALIZED = status;
        log.info("TestController updateInitStatus end, INITIALIZED:{}, status:{}", SyncFetchFileTask.INITIALIZED, status);
        return SyncFetchFileTask.INITIALIZED;
    }
}
