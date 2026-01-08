package com.dtxytech.powerdatacollect.core.service.power;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;

/**
 * @Author zay
 * @Date 2025/12/18 16:15
 */
public interface PowerForecastDataService {

    int insertData(PowerForecastData obj);

    boolean checkDuplicate(PowerForecastData obj);
}
