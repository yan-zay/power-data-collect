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
@TableName(value = "sne_power_forecast_data_0002", autoResultMap = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerForecastData2 {

    private String stationId;

    @TableField(value = "station_code")
    private String stationCode;

    @TableField(value = "indicator_type")
    private String indicatorType;

    @TableField(value = "forecast_time_str")
    private String forecastTimeStr;

    @TableField(value = "forecast_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime forecastTime;

    @TableField(value = "file_path")
    private String filePath;

    @TableField(value = "file_name")
    private String fileName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private String forecastData;
}
