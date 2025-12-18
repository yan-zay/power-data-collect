package com.dtxytech.powerdatacollect.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author zay
 * @Date 2025/12/15 19:36
 */
@Getter
@AllArgsConstructor
public enum StationEnum {

//    DQ("DQ", "SHORT_POWER", "短期预测功率"),
    CDQ("CDQ", "VERY_SHORT_POWER", "超短期预测功率"),

    ;

    private final String name;
    private final String value;
    private final String desc;
}
