package com.dtxytech.powerdatacollect.service.power;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dtxytech.powerdatacollect.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.mapper.PowerForecastDataMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @Author zay
 * @Date 2025/12/18 16:16
 */
@Service
@AllArgsConstructor
public class PowerForecastDataServiceImpl implements PowerForecastDataService {

    private PowerForecastDataMapper powerForecastDataMapper;

    @Override
    public int insertData(PowerForecastData obj) {
        return powerForecastDataMapper.insert(obj);
    }

    @Override
    public boolean checkDuplicate(PowerForecastData powerForecastData) {
        LambdaQueryWrapper<PowerForecastData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PowerForecastData::getStationCode, powerForecastData.getStationCode())
                .eq(PowerForecastData::getIndicatorType, powerForecastData.getIndicatorType())
                .eq(PowerForecastData::getForecastTime, powerForecastData.getForecastTime());
        Long count = powerForecastDataMapper.selectCount(wrapper);
        return count >= 1;
    }
}
