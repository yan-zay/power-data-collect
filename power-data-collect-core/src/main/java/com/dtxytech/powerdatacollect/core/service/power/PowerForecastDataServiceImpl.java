package com.dtxytech.powerdatacollect.core.service.power;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dtxytech.powerdatacollect.core.config.SftpProperties;
import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.entity.guangxi.PowerForecastDataCdt;
import com.dtxytech.powerdatacollect.core.entity.guangxi.PowerForecastDataGgep;
import com.dtxytech.powerdatacollect.core.entity.guangxi.PowerForecastDataStation;
import com.dtxytech.powerdatacollect.core.mapper.PowerForecastDataMapper;
import com.dtxytech.powerdatacollect.core.service.power.guangxi.PowerForecastDataServiceCdt;
import com.dtxytech.powerdatacollect.core.service.power.guangxi.PowerForecastDataServiceGgep;
import com.dtxytech.powerdatacollect.core.service.power.guangxi.PowerForecastDataServiceStation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author zay
 * @Date 2025/12/18 16:16
 */
@Slf4j
@Service
@AllArgsConstructor
public class PowerForecastDataServiceImpl extends ServiceImpl<PowerForecastDataMapper, PowerForecastData> implements PowerForecastDataService {

    private PowerForecastDataMapper powerForecastDataMapper;
    private PowerForecastDataServiceCdt cdt;
    private PowerForecastDataServiceGgep ggep;
    private PowerForecastDataServiceStation station;
    private SftpProperties sftpProperties;

    @Override
    @Transactional
    public void saveList(List<PowerForecastData> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        if (checkRegionGuangxi()) {
            saveListByType(list);
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

    private boolean checkRegionGuangxi() {
        return "guangxi".equals(sftpProperties.getRegion());
    }

    @Override
    @Transactional
    public void saveListByType(List<PowerForecastData> list) {
        String filePath = list.get(0).getFilePath();
        if (filePath.startsWith("//cdt")) {
            cdt.saveBatch(copyList(list, PowerForecastDataCdt.class));
        }else if (filePath.startsWith("//ggep")) {
            ggep.saveBatch(copyList(list, PowerForecastDataGgep.class));
        }else if (filePath.startsWith("//station")) {
            station.saveBatch(copyList(list, PowerForecastDataStation.class));
        }
    }

    public static <T> List<T> copyList(List<?> sourceList, Class<T> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }

        return sourceList.stream()
                .map(item -> copyObject(item, targetClass))
                .collect(Collectors.toList());
    }

    private static <T> T copyObject(Object source, Class<T> targetClass) {
        try {
            T target = targetClass.newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to copy object", e);
        }
    }
}
