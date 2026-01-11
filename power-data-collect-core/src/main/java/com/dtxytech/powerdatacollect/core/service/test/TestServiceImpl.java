package com.dtxytech.powerdatacollect.core.service.test;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.mapper.PowerForecastDataMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
        PowerForecastData obj = PowerForecastData.builder()
                .collectTime(LocalDateTime.now())
                .forecastTime(LocalDateTime.now())
                .stationCode(dto.get("stationCode"))
                .indexCode(dto.get("indexCode"))
                .energyType(dto.get("energyType"))

                .assetCode(dto.get("assetCode"))
                .forecastValue(new BigDecimal(dto.get("forecastValue")))
                .orderNo(-1)
                .filePath(dto.get("filePath"))
                .fileName(dto.get("fileName"))
                .createTime(LocalDateTime.now())
                .build();
        int insert = powerForecastDataMapper.insert(obj);
        return insert + "";
    }
}
