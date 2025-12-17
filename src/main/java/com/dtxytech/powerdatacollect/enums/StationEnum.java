package com.dtxytech.powerdatacollect.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

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

//    public static final List<String> list =

    private final String name;
    private final String value;
    private final String desc;
}
