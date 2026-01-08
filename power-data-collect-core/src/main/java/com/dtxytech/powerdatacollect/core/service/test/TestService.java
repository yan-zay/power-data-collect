package com.dtxytech.powerdatacollect.core.service.test;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData2;

import java.util.List;
import java.util.Map;

/**
 * @Author zay
 * @Date 2025/12/12 14:23
 */
public interface TestService {
    void test02(Map<String, String> dto);

    String insertData(Map<String, String> dto);

    String insertData2(Map<String, String> dto);

    List<PowerForecastData2> getData2();
}
