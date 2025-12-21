package com.dtxytech.powerdatacollect.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author zay
 * @Date 2025/12/15 20:29
 */
@Data
@TableName(value = "sne_power_forecast_data", autoResultMap = true) // ⭐ 必须开启 autoResultMap
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerForecastData {

    @TableField(value = "station_code")
    private String stationCode;

    @TableField(value = "indicator_type")
    private String indicatorType;

    @TableField(value = "forecast_time_str")
    private String forecastTimeStr;

    @TableField(value = "forecast_time")
    private String forecastTime;

    @TableField(value = "file_path")
    private String filePath;

    @TableField(value = "file_name")
    private String fileName;

    private String createTime;

    @TableField(value = "forecast_data", typeHandler = JacksonTypeHandler.class)
    private List<String> forecastData;
}
