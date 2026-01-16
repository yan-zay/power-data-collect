package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.service.station.StationService;
import com.jcraft.jsch.ChannelSftp;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
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
@ConditionalOnProperty(name = "sftp.region", havingValue = "neimeng", matchIfMissing = false)
public class SftpFileParserNeimeng extends SftpFileParser {

    private StationService stationService;

    @Override
    public List<PowerForecastData> parseFile(IndicatorTypeEnum indicatorType, ChannelSftp sftp, String path) {
        // 从路径中提取文件名和目录信息
        String fileName = getFileName(path);
        IndicatorTypeEnum indicatorType2 = determineIndicatorTypeFromFileName(fileName);
        if (indicatorType == null) {
            log.warn("无法确定指标类型，跳过文件: {}", path);
            return new ArrayList<>();
        }

        try (InputStream in = sftp.get(path)) {
            return doParseFile(indicatorType, in, path, fileName);
        } catch (Exception e) {
            log.error("SftpFileParserNeimeng 解析文件失败: {}", path, e);
            return new ArrayList<>();
        }
    }

    /**
     * 从文件名确定指标类型
     */
    private IndicatorTypeEnum determineIndicatorTypeFromFileName(String fileName) {
        for (IndicatorTypeEnum type : IndicatorTypeEnum.values()) {
            if (type.checkFileName(fileName)) {
                return type;
            }
        }
        return null;
    }

    public List<PowerForecastData> doParseFile(IndicatorTypeEnum indicatorType, InputStream in,
                                                             String filePath, String filename) {
        // === 2. 流式读取并解析 ===
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            String line;
            boolean inDataBlock = false;
            List<String> dataLines = new ArrayList<>();
            String forecastTimeStr = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) continue;

                // 跳过注释行
                if (line.startsWith("//")) {
                    continue;
                }

                // 解析 Entity 行，提取 type 和 time
                if (line.startsWith("<!") && line.contains("time=")) {
                    forecastTimeStr = getEntityTime(line);
                    continue;
                }

                // 进入数据块
                if (line.contains("<forecastdata::")) {
                    inDataBlock = true;
                    continue;
                }

                // 离开数据块
                if (line.equals("</forecastdata::DTCG>")) {
                    inDataBlock = false;
                    continue;
                }

                // 跳过表头行（以 @ 开头）
                if (inDataBlock && line.startsWith("@")) {
                    continue;
                }

                // 收集有效数据行（以 # 开头）
                if (inDataBlock && line.startsWith("#")) {
                    String[] cols = line.split("\t");
                    if (cols.length >= 2) {
                        dataLines.add(cols[1]); // "0.65"
                    }
                }
            }
            // 推断 stationId：从 fileName 提取前缀（如 DTDL4_... → DTDL4）
            String stationCode = getPathPart(filePath, 4);
            log.info("doParseFile stationCode:{}", stationCode);

            return getListDate(indicatorType, filePath, filename, stationCode, forecastTimeStr, dataLines);
        } catch (Exception e) {
            log.error("SftpFileParserNeimeng doParseFile, filePath:{}, 读取或解析文件失败: {}", filePath, e.getMessage(), e);
            return null;
        }
    }

    protected List<PowerForecastData> getListDate(IndicatorTypeEnum indicatorType, String filePath, String filename,
                                                  String stationCode, String forecastTimeStr, List<String> dataLines) {
        List<PowerForecastData> result = new ArrayList<>();
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
                log.error("SftpFileParserNeimeng getListDate value:{}", value, e);
                continue;
            }
            PowerForecastData obj = PowerForecastData.builder()
                    .collectTime(collectTime)
                    .forecastTime(forecastTime)
                    .stationCode(stationCode)
                    .indexCode(indicatorType.getValue())
                    .energyType("energyType")//?

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

    protected String getEntityTime(String line) {
        Pattern timePattern = Pattern.compile("time='([\\d-_:]+)'");
        Matcher matcher = timePattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("无法解析时间字符串: " + line);
    }

    protected String getPathPart(String filePath, int part) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        String[] parts = filePath.split("/");
        if (parts.length >= part + 1) { // 路径以/开头会产生一个空的第一项
            return parts[part]; // 第五项实际上是第四级目录
        }
        log.error("SftpFileParserNeimeng Invalid file filePath:{}, part:{}", filePath, part);
        return "";
    }
}
