package com.dtxytech.powerdatacollect.core.service.power;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Author zay
 * @Date 2025/12/18 16:15
 */
public interface PowerForecastDataService {

    void saveList(List<PowerForecastData> list);

    @Transactional
    void saveListByType(List<PowerForecastData> list);

    boolean checkDuplicate(PowerForecastData obj);
}
