package com.dtxytech.powerdatacollect.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zzl
 * @version 1.0
 * @date 2026/1/13 17:15
 * @description
 */
@Getter
@AllArgsConstructor
public enum EnergyTypeEnum {
    //风电
    WIND("WIND", "风电"),
    //光伏
    PV("PV", "光伏");


    private final String code;
    private final String name;
}
