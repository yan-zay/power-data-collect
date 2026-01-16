package com.dtxytech.powerdatacollect.core.service.sftp;

import com.alibaba.fastjson2.JSON;
import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.enums.EnergyTypeStationCodeEnum;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.service.station.StationService;
import com.jcraft.jsch.ChannelSftp;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author zay
 * @Date 2025/12/16 17:26
 */
@Slf4j
@Component
@AllArgsConstructor
@ConditionalOnProperty(name = "sftp.region", havingValue = "liaoning", matchIfMissing = false)
public class SftpFileParserLiaoning extends SftpFileParser {

    private StationService stationService;

    @Override
    public List<PowerForecastData> parseFile(IndicatorTypeEnum indicatorType, ChannelSftp sftp, String path) {
        // 从路径中提取文件名和目录信息
        String fileName = getFileName(path);

        try (InputStream in = sftp.get(path)) {
            return doParseFile(in, path, fileName);
        } catch (Exception e) {
            log.error("SftpFileParserNeimeng 解析文件失败: {}", path, e);
            return new ArrayList<>();
        }
    }

    /**
     * 从远程 SFTP 读取并解析预测数据文件（适配两列格式）
     */
    public List<PowerForecastData> doParseFile(InputStream in, String filePath, String filename) {
        // === 2. 流式读取并解析 ===
        try {
            List<String> dataLines = new ArrayList<>();
            String forecastTimeStr = null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(reader);
            Element rootElement = document.getRootElement();
            List<Element> rowList = rootElement.elements("Row");
            for (Element rowElement : rowList) {
                String YG = rowElement.elementTextTrim("YG");
                dataLines.add(YG);
            }
            // 推断 stationId：从 fileName 提取前缀（如 DTDL4_... → DTDL4）
            String stationCode = getPathPart(filePath, 4);
            log.info("doParseFile dataLines:jsonStr:{}", JSON.toJSONString(dataLines));
            log.info("doParseFile stationCode:{}", stationCode);

            return getListDate(filePath, filename, stationCode, forecastTimeStr, dataLines);
        } catch (Exception e) {
            log.error("读取或解析文件失败 SftpFileParserLiaoning e:{}", e.getMessage(), e);
            return null;
        }
    }

    protected List<PowerForecastData> getListDate(String filePath, String filename,
                                                  String stationCode, String forecastTimeStr, List<String> dataLines) {
        List<PowerForecastData> result = new ArrayList<>();
        String[] parts = filePath.split("[\\\\/]");
        String lastPart = parts[parts.length - 1];
        String[] split = filename.split("_");
        String indexCode="";
        if(IndicatorTypeEnum.DQ.getName().toUpperCase().equals(lastPart)==true){
            forecastTimeStr=split[1];
            indexCode=IndicatorTypeEnum.DQ.getValue();
        }else if(IndicatorTypeEnum.CDQ.getName().toUpperCase().equals(lastPart)==true){
            forecastTimeStr=split[1]+split[split.length-1].replace(".xml","");
            indexCode=IndicatorTypeEnum.CDQ.getValue();
        }else {
            if(dataLines.size()==16){
                indexCode=IndicatorTypeEnum.CDQ.getValue();
            }else if(dataLines.size()==96){
                indexCode=IndicatorTypeEnum.DQ.getValue();
            }else {
                log.error("未获取到指标类型", lastPart);
            }
        }
        LocalDateTime collectTime = parseForecastTimeStr(forecastTimeStr);
        LocalDateTime forecastTime = parseForecastTimeStr(forecastTimeStr);
        String stationId = stationService.getStationIdByCode(stationCode);
        log.info("doParseFile getListDate stationCode:{}， stationId:{}", stationCode, stationId);
        for (int i = 0; i < dataLines.size(); i++) {
            String data = dataLines.get(i);
            BigDecimal value = null;
            try {
                value = new BigDecimal(data);
            }catch (Exception e) {
                log.error("SftpFileParser getListDate value:{}", value, e);
                continue;
            }
            PowerForecastData obj = PowerForecastData.builder()
                    .collectTime(collectTime)
                    .forecastTime(forecastTime)
                    .stationCode(stationCode)
                    .indexCode(indexCode)
                    .energyType(EnergyTypeStationCodeEnum.getByStationCode(stationCode))//?

                    .assetCode(stationId)
                    .forecastValue(value)
                    .orderNo(i + 1)

                    .filePath(filePath)
                    .fileName(filename)
                    .createTime(LocalDateTime.now())
                    .build();
            forecastTime = forecastTime.plusMinutes(15);
            result.add(obj);
        }
        return result;
    }

    private static String getEntityTime(String line) {
        Pattern timePattern = Pattern.compile("time='([\\d-_:]+)'");
        Matcher matcher = timePattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("无法解析时间字符串: " + line);
    }


    /**
     * 根据指标类型统一解析日期格式为 LocalDateTime
     * @param forecastTimeStr 原始时间字符串
     * @return 解析后的 LocalDateTime 对象
     */
    @Override
    public LocalDateTime parseForecastTimeStr(String forecastTimeStr) {
        if (forecastTimeStr == null || forecastTimeStr.isEmpty()) {
            return null;
        }
        // 处理纯数字格式 yyyymmddhhmm
        if (forecastTimeStr.matches("\\d{12}")) {
            return LocalDateTime.parse(forecastTimeStr,
                    DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        }

        String normalizedTimeStr = forecastTimeStr.replace("_", " ");

        // 根据字符串长度判断是哪种格式
        if (normalizedTimeStr.length() == 16) {  // "yyyy-MM-dd_HH:mm" 格式 (CDQ类型)
            String fullTimeStr = normalizedTimeStr + ":00"; // 补充秒数 ":00"
            return LocalDateTime.parse(fullTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else if (normalizedTimeStr.length() == 19) {  // "yyyy-MM-dd_HH:mm:ss" 格式 (DQ类型)
            return LocalDateTime.parse(normalizedTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        // 尝试解析其他格式的时间字符串
        try {
            return LocalDateTime.parse(normalizedTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            log.error("无法解析时间字符串: {}", normalizedTimeStr, e);
            throw new IllegalArgumentException("parseForecastTimeStr 无法解析时间字符串: " + normalizedTimeStr);
        }
    }

    public String getPathPart(String filePath, int part) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        String[] parts = filePath.split("/");
        // 获取倒数第二部分（目录名）
        if (parts.length >= 2) {
            return parts[parts.length - 2];
        }
        log.error("Invalid file filePath:{}, part:{}", filePath, part);
        return "";
    }
}
