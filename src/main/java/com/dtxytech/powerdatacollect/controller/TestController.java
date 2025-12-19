package com.dtxytech.powerdatacollect.controller;

import com.dtxytech.powerdatacollect.service.test.TestService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
