package com.dtxytech.powerdatacollect.core.service.test;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.mapper.PowerForecastDataMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    @Override
    public void test02(Map<String, String> dto) {
        log.info("test02: {}", dto);
    }

    @Override
    public String insertData(Map<String, String> dto) {
        PowerForecastData build = PowerForecastData.builder()
                .stationCode(dto.get("stationCode"))
                .indexCode(dto.get("indicatorType"))
                .forecastTime(LocalDateTime.now())
                .filePath(dto.get("filePath"))
                .fileName(dto.get("fileName"))
                .createTime(LocalDateTime.now())
                .forecastValue("44.44")
                .build();
        int insert = powerForecastDataMapper.insert(build);
        return insert + "";
    }
}
