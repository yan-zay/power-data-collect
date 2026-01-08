package com.dtxytech.powerdatacollect.core.service.station;

import com.alibaba.fastjson2.JSON;
import com.dtxytech.powerdatacollect.core.pojo.AssetStationVO;
import com.dtxytech.powerdatacollect.core.utils.OkHttpUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author zay
 * @Date 2026/1/8 16:43
 */
@Slf4j
@Service
@AllArgsConstructor
public class StationServiceImpl implements StationService {

    private final OkHttpUtil okHttpUtil;

    @Override
    public String getStationIdByCode(String stationCode) {
        return null;
/*        String body = getData(stationCode);
        if (body == null) {
            return null;
        }
        List<AssetStationVO> list = JSON.parseArray(body, AssetStationVO.class);
        AssetStationVO assetStationVO = list.stream().filter(obj -> obj.getCode().equals(stationCode)
                && obj.getDefaultValue().getValue().equals(stationCode)).findFirst().orElse(null);
        return assetStationVO == null ? null : assetStationVO.getAssetId();*/
    }

    public String getData(String stationCode) {
//        String url = systemConfig.getObsPointServerUrl() + systemConfig.getObsPointApiUrl();
        String url = "http://127.0.0.1:41104/";

        // 构建请求体
        Map<String, Object> map = new HashMap<>();
        map.put("code", "station_code_predpwr");
        map.put("defaultValue.value", stationCode);

        try {
            return okHttpUtil.get(url, map);
        } catch (Exception e) {
            log.error("StationServiceImpl 调用观测点数据查询接口失败, url:{}, map: {}", url, map, e);
            return null;
        }
    }
}
