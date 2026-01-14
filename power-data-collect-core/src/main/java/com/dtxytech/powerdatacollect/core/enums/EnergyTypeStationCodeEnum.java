package com.dtxytech.powerdatacollect.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zzl
 * @version 1.0
 * @date 2026/1/13 17:10
 * @description
 */
@Getter
@AllArgsConstructor
public enum EnergyTypeStationCodeEnum {
    SHIJIANFANG(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"shijianfang_211581","十间房风电场"),
    WULONGSHAN(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"wulongshan_211503","五龙山风电场"),
    MANJING(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"manjing_211505","满井风电场"),
    HONGSHAN(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"hongshan_211506","红山风电场"),
    TAIYANGSHAN(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"taiyangshan_211507","太阳山风电场"),
    SANJIANGKOU(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"sanjiangkou_211509","三江口风电场"),
    SHIJINGAO(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"shijingao_211513","石金皋风电场"),
    WIND_211512(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"211512","查台风电场"),
    PV_1NLB(EnergyTypeEnum.PV.getCode(),EnergyTypeEnum.PV.getName(),"1nlb","查台风电场"),
    ANTAI(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"antai_211515","安台风电场"),
    LNHP(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"lnhp","海派风电场"),
    INND(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"Innd","南岛风电场"),
    LNSJZ(EnergyTypeEnum.PV.getCode(),EnergyTypeEnum.WIND.getName(),"lnsjz","三家子光伏电站"),
    LNZSJ(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"lnzsj","中三家风电场"),
    LNSM(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"lnsm","双庙风电场"),
    LNGYZ(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"lngyz","公营子风光电站"),
    LNXL(EnergyTypeEnum.PV.getCode(),EnergyTypeEnum.WIND.getName(),"lnxl","公营子风光电站"),
    LNYSG(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"lnysg","杨树沟风电场"),
    LNXTZ(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"lnxtz","小塔子风电场"),
    LNSYZ(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"lnsyz","石营子风电场"),
    LNSLS(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"lnsls","三棱山风电场"),
    LNTX(EnergyTypeEnum.PV.getCode(),EnergyTypeEnum.WIND.getName(),"lntx","唐新光伏电站"),
    LNGQ(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"lngq","高桥风电场"),
    LNWP(EnergyTypeEnum.WIND.getCode(),EnergyTypeEnum.WIND.getName(),"lnwp","茶家风电场");




    private final String energyType;
    private final String energyTypeName;
    private final String stationCode;
    private final String stationName;


    public static String getByStationCode(String stationCode) {
        for (EnergyTypeStationCodeEnum value : EnergyTypeStationCodeEnum.values()) {
            if (value.getStationCode().equals(stationCode)) {
                return value.getEnergyType();
            }
        }
        return null;
    }
}
