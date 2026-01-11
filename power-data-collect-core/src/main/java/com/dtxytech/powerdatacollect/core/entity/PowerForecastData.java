package com.dtxytech.powerdatacollect.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime forecastTime;
    @TableField(value = "station_code")
    private String stationCode;
    @TableField(value = "index_code")
    private String indexCode;
    @TableField(value = "energy_type")
    private String energyType;

    @TableField(value = "asset_code")
    private String assetCode;
    @TableField(value = "forecast_value")
    private BigDecimal forecastValue;
    @TableField(value = "order_no")
    private Integer orderNo;
    @TableField(value = "file_path")
    private String filePath;
    @TableField(value = "file_name")
    private String fileName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
