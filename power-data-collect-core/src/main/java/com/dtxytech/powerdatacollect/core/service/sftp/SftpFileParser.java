package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.service.station.StationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @Author zay
 * @Date 2025/12/16 17:26
 */
@Slf4j
public abstract class SftpFileParser {

    @Autowired
    protected StationService stationService;

    public abstract List<PowerForecastData> parseFile(String path);

    protected String getFileName(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * 根据指标类型统一解析日期格式为 LocalDateTime
     * @param forecastTimeStr 原始时间字符串
     * @return 解析后的 LocalDateTime 对象
     */
    protected LocalDateTime parseForecastTimeStr(String forecastTimeStr) {
        if (forecastTimeStr == null || forecastTimeStr.isEmpty()) {
            return null;
        }
        String normalizedTimeStr = forecastTimeStr.replace("_", " ");

        // 根据字符串长度判断是哪种格式
        if (normalizedTimeStr.length() == 16) {  // "yyyy-MM-dd_HH:mm" 格式 (CDQ类型)
            String fullTimeStr = normalizedTimeStr + ":00"; // 补充秒数 ":00"
            return LocalDateTime.parse(fullTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else if (normalizedTimeStr.length() == 19) {  // "yyyy-MM-dd_HH:mm:ss" 格式 (DQ类型)
            return LocalDateTime.parse(normalizedTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        // 尝试解析其他格式的时间字符串
        try {
            return LocalDateTime.parse(normalizedTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            log.error("无法解析时间字符串: {}", normalizedTimeStr, e);
            throw new IllegalArgumentException("parseForecastTimeStr 无法解析时间字符串: " + normalizedTimeStr);
        }
    }
}
