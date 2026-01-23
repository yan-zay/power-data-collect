package com.dtxytech.powerdatacollect.core.service.power.guangxi;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dtxytech.powerdatacollect.core.entity.guangxi.PowerForecastDataStation;
import com.dtxytech.powerdatacollect.core.mapper.guangxi.PowerForecastDataMapperStation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Author zay
 * @Date 2026/1/14 10:23
 */
@Slf4j
@Service
public class PowerForecastDataServiceStation extends ServiceImpl<PowerForecastDataMapperStation, PowerForecastDataStation> {

    @Transactional
    public void saveList(List<PowerForecastDataStation> list) {
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

    @Transactional(readOnly = true)
    public boolean checkDuplicate(PowerForecastDataStation powerForecastData) {
        LambdaQueryWrapper<PowerForecastDataStation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PowerForecastDataStation::getStationCode, powerForecastData.getStationCode())
                .eq(PowerForecastDataStation::getIndexCode, powerForecastData.getIndexCode())
                .eq(PowerForecastDataStation::getCollectTime, powerForecastData.getCollectTime());
        Long count = this.baseMapper.selectCount(wrapper);
        return count >= 1;
    }
}
