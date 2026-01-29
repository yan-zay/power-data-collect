package com.dtxytech.powerdatacollect.core.service.power;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.mapper.PowerForecastDataMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Author zay
 * @Date 2025/12/18 16:16
 */
@Slf4j
@Service
@AllArgsConstructor
public class PowerForecastDataServiceImpl extends ServiceImpl<PowerForecastDataMapper, PowerForecastData> implements PowerForecastDataService {

    private PowerForecastDataMapper powerForecastDataMapper;

    @Override
    @Transactional(readOnly = true)
    public boolean checkFileExists(IndicatorTypeEnum indicatorType, String path) {
        LambdaQueryWrapper<PowerForecastData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PowerForecastData::getIndexCode, indicatorType.getValue())
                .eq(PowerForecastData::getFilePath, path);
        Long count = powerForecastDataMapper.selectCount(wrapper);
        return count >= 1;
    }

    @Override
    @Transactional
    public void saveList(List<PowerForecastData> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        boolean exist = this.checkDuplicate(list.get(0));
        if (exist) {
            log.info("powerForecastDataService.checkDuplicate exist, list.get(0):{}", list.get(0));
            return;
        }
        boolean saved = this.saveBatch(list);
        if (!saved) {
            log.error("powerForecastDataService.saveBatch error, list:{}", list);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkDuplicate(PowerForecastData powerForecastData) {
        LambdaQueryWrapper<PowerForecastData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PowerForecastData::getCollectTime, powerForecastData.getCollectTime())
                .eq(PowerForecastData::getForecastTime, powerForecastData.getForecastTime())
                .eq(PowerForecastData::getStationCode, powerForecastData.getStationCode())
                .eq(PowerForecastData::getIndexCode, powerForecastData.getIndexCode())
                .eq(PowerForecastData::getEnergyType, powerForecastData.getEnergyType());
        Long count = powerForecastDataMapper.selectCount(wrapper);
        return count >= 1;
    }
}
