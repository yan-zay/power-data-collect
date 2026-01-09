package com.dtxytech.powerdatacollect.core.service.power;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.mapper.PowerForecastDataMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @Author zay
 * @Date 2025/12/18 16:16
 */
@Service
@AllArgsConstructor
public class PowerForecastDataServiceImpl extends ServiceImpl<PowerForecastDataMapper, PowerForecastData> implements PowerForecastDataService {

    private PowerForecastDataMapper powerForecastDataMapper;

    @Override
    public int insertData(PowerForecastData obj) {
        return powerForecastDataMapper.insert(obj);
    }

    @Override
    public boolean checkDuplicate(PowerForecastData powerForecastData) {
        LambdaQueryWrapper<PowerForecastData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PowerForecastData::getStationCode, powerForecastData.getStationCode())
                .eq(PowerForecastData::getIndexCode, powerForecastData.getIndexCode())
                .eq(PowerForecastData::getCollectTime, powerForecastData.getCollectTime());
        Long count = powerForecastDataMapper.selectCount(wrapper);
        return count >= 1;
    }
}
