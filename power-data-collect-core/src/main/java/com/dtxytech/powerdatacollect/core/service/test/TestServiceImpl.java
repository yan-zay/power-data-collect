package com.dtxytech.powerdatacollect.core.service.test;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.entity.PowerForecastData2;
import com.dtxytech.powerdatacollect.core.mapper.PowerForecastDataMapper;
import com.dtxytech.powerdatacollect.core.mapper.PowerForecastDataMapper2;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Author zay
 * @Date 2025/12/12 14:23
 */
@Slf4j
@Service
@AllArgsConstructor
public class TestServiceImpl implements TestService {

    private PowerForecastDataMapper powerForecastDataMapper;
    private PowerForecastDataMapper2 powerForecastDataMapper2;

    @Override
    public void test02(Map<String, String> dto) {
        log.info("test02: {}", dto);
    }

    @Override
    public String insertData(Map<String, String> dto) {
        PowerForecastData build = PowerForecastData.builder()
                .stationCode(dto.get("stationCode"))
                .indicatorType(dto.get("indicatorType"))
                .forecastTime(LocalDateTime.now())
                .forecastTimeStr(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .filePath(dto.get("filePath"))
                .fileName(dto.get("fileName"))
                .createTime(LocalDateTime.now())
                .forecastData("44.44")
                .build();
        int insert = powerForecastDataMapper.insert(build);
        return insert + "";
    }

    @Override
    public String insertData2(Map<String, String> dto) {
        PowerForecastData2 build = PowerForecastData2.builder()
                .stationId(dto.get("stationId"))
                .stationCode(dto.get("stationCode"))
                .indicatorType(dto.get("indicatorType"))
                .forecastTimeStr(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .forecastTime(LocalDateTime.now())
                .filePath(dto.get("filePath"))
                .fileName(dto.get("fileName"))
                .createTime(LocalDateTime.now())
                .forecastData("44.44")
                .build();
        int insert = powerForecastDataMapper2.insert(build);
        return insert + "";
    }

    @Override
    public List<PowerForecastData2> getData2() {
        return powerForecastDataMapper2.selectList(null);
    }
}
