package com.dtxytech.powerdatacollect.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author zay
 * @Date 2025/12/15 20:29
 */
@Data
@TableName(value = "sne_power_forecast_data", autoResultMap = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerForecastData {

    @TableField(value = "station_code")
    private String stationCode;

    private String indicatorType;

    private String forecastTimeStr;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime forecastTime;

    @TableField(value = "station_id")
    private String stationId;

    private String filePath;

    private String fileName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private String forecastData;
}
