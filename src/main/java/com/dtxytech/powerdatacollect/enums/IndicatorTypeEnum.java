package com.dtxytech.powerdatacollect.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author zay
 * @Date 2025/12/15 11:22
 */
@Getter
@AllArgsConstructor
public enum IndicatorTypeEnum {

    DQ("DQ", "SHORT_POWER", "短期预测功率"),
    CDQ("CDQ", "VERY_SHORT_POWER", "短期预测功率"),

    ;

    private final String name;
    private final String value;
    private final String desc;

    public boolean checkFileName(String fileName) {
        fileName = fileName.toUpperCase();
        if (this == DQ) {
            return fileName.contains(this.name) && !fileName.contains(CDQ.name);
        }else if (this == CDQ) {
            return fileName.contains(this.name);
        }
        throw new RuntimeException("IndicatorTypeEnum.checkFileName: " + this.name);
    }
}
