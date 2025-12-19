package com.dtxytech.powerdatacollect.service.power;

import com.dtxytech.powerdatacollect.entity.PowerForecastData;

/**
 * @Author zay
 * @Date 2025/12/18 16:15
 */
public interface PowerForecastDataService {

    int insertData(PowerForecastData obj);

    boolean checkDuplicate(PowerForecastData powerForecastData);
}
