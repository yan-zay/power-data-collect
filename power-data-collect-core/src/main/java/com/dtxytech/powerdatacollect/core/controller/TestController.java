package com.dtxytech.powerdatacollect.core.controller;

import com.dtxytech.powerdatacollect.core.service.test.TestService;
import com.dtxytech.powerdatacollect.core.task.SyncFetchFileTask;
import lombok.AllArgsConstructor;
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
}
