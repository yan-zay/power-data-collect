package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.service.station.StationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@ConditionalOnProperty(name = "sftp.region", havingValue = "longjiang", matchIfMissing = false)
public class SftpFileParserLongjiang extends SftpFileParser {

    private StationService stationService;

    @Override
    public List<PowerForecastData> parseFile(String path) {
        // 从路径中提取文件名和目录信息
        String fileName = getFileName(path);
        IndicatorTypeEnum indicatorType = determineIndicatorTypeFromFileName(fileName);
        if (indicatorType == null) {
            log.warn("无法确定指标类型，跳过文件: {}", path);
            return new ArrayList<>();
        }

        try (InputStream in = Files.newInputStream(Paths.get(path))) {
            return parseForecastFileFromSftp(indicatorType, in, path, fileName);
        } catch (Exception e) {
            log.error("解析文件失败: {}", path, e);
            return new ArrayList<>();
        }
    }

    /**
     * 从文件名确定指标类型
     */
    private IndicatorTypeEnum determineIndicatorTypeFromFileName(String fileName) {
        if (fileName != null) {
            fileName = fileName.toUpperCase();
            if (fileName.contains(IndicatorTypeEnum.DQ.getName()) && !fileName.contains(IndicatorTypeEnum.CDQ.getName())) {
                return IndicatorTypeEnum.DQ;
            } else if (fileName.contains(IndicatorTypeEnum.CDQ.getName())) {
                return IndicatorTypeEnum.CDQ;
            }
        }
        return null;
    }

    /**
     * 从远程 SFTP 读取并解析预测数据文件（黑龙江地区特殊格式）
     * 解析包含 DQ 和 CDQ 后缀的文件
     */
    public List<PowerForecastData> parseForecastFileFromSftp(IndicatorTypeEnum indicatorType, InputStream in,
                                                             String filePath, String filename) {
        // === 2. 流式读取并解析 ===
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            String line;
            String forecastTimeStr = null;
            String stationCode = null;
            List<PowerForecastData> result = new ArrayList<>();

            // 从文件名提取站点代码（例如：FAFD70_20260108_0000_CDQ.WPD -> FAFD70）
            stationCode = extractStationCodeFromFile(filename);
            
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

                // 根据指标类型选择解析特定的数据块
                if (indicatorType == IndicatorTypeEnum.DQ && line.contains("<ShortTermForcast::")) {
                    result.addAll(parseShortTermForcastBlock(reader, indicatorType, filePath, filename, stationCode, forecastTimeStr));
                } else if (indicatorType == IndicatorTypeEnum.CDQ && line.contains("<UltraShortTermForcast_P2P::")) {
                    result.addAll(parseUltraShortTermForcastP2PBlock(reader, indicatorType, filePath, filename, stationCode, forecastTimeStr));
                }
            }

            log.info("parseForecastFileFromSftp stationCode:{}, result size:{}", stationCode, result.size());

            return result;
        } catch (Exception e) {
            log.error("❌ 读取或解析文件失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 从文件名提取站点代码
     */
    private String extractStationCodeFromFile(String filename) {
        // 文件名格式如: FAFD70_20260108_0000_CDQ.WPD
        if (filename != null && filename.contains("_")) {
            return filename.substring(0, filename.indexOf("_"));
        }
        return "";
    }

    /**
     * 解析短期预测功率数据块 (DQ - ShortTermForcast)
     */
    private List<PowerForecastData> parseShortTermForcastBlock(BufferedReader reader, IndicatorTypeEnum indicatorType, 
                                                            String filePath, String filename, String stationCode, 
                                                            String forecastTimeStr) throws java.io.IOException {
        List<PowerForecastData> result = new ArrayList<>();
        String line;
        LocalDateTime baseForecastTime = parseForecastTimeStr(forecastTimeStr);
        int orderNo = 1;
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            
            if (line.equals("</ShortTermForcast::" + stationCode + ">")) {
                break; // 结束当前数据块
            }
            
            if (line.startsWith("#")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) { // 包含序号和预测值
                    String valueStr = parts[1];
                    
                    try {
                        BigDecimal value = new BigDecimal(valueStr);
                        
                        PowerForecastData obj = buildPowerForecastData(
                            indicatorType, filePath, filename, stationCode, 
                            forecastTimeStr, value, orderNo, getEnergyTypeFromFile(filename));
                            
                        // 短期预测通常是未来15分钟间隔的数据
                        obj.setForecastTime(baseForecastTime.plusMinutes((orderNo - 1) * 15L));
                            
                        result.add(obj);
                        orderNo++;
                    } catch (NumberFormatException e) {
                        log.warn("无法解析短期预测功率数据: {}", line);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 解析超短期预测功率P2P数据块 (CDQ - UltraShortTermForcast_P2P)
     */
    private List<PowerForecastData> parseUltraShortTermForcastP2PBlock(BufferedReader reader, IndicatorTypeEnum indicatorType, 
                                                                 String filePath, String filename, String stationCode, 
                                                                 String forecastTimeStr) throws java.io.IOException {
        List<PowerForecastData> result = new ArrayList<>();
        String line;
        LocalDateTime baseForecastTime = parseForecastTimeStr(forecastTimeStr);
        int orderNo = 1;
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            
            if (line.equals("</UltraShortTermForcast_P2P::" + stationCode + ">")) {
                break; // 结束当前数据块
            }
            
            if (line.startsWith("#")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) { // 包含序号和预测值
                    String valueStr = parts[1];
                    
                    try {
                        BigDecimal value = new BigDecimal(valueStr);
                        
                        PowerForecastData obj = buildPowerForecastData(
                            indicatorType, filePath, filename, stationCode, 
                            forecastTimeStr, value, orderNo, getEnergyTypeFromFile(filename));
                            
                        // 超短期预测通常是未来15分钟间隔的数据
                        obj.setForecastTime(baseForecastTime.plusMinutes((orderNo - 1) * 15L));
                            
                        result.add(obj);
                        orderNo++;
                    } catch (NumberFormatException e) {
                        log.warn("无法解析超短期预测功率P2P数据: {}", line);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 根据文件扩展名确定energyType
     */
    private String getEnergyTypeFromFile(String filename) {
        if (filename != null) {
            filename = filename.toLowerCase();
            if (filename.endsWith(".wpd")) {
                return "wind";
            } else if (filename.endsWith(".ppd")) {
                return "pv";
            }
        }
        return "unknown";
    }

    /**
     * 构建PowerForecastData对象
     */
    private PowerForecastData buildPowerForecastData(IndicatorTypeEnum indicatorType, String filePath, 
                                                     String filename, String stationCode, 
                                                     String forecastTimeStr, BigDecimal value, 
                                                     Integer orderNo, String energyType) {
        LocalDateTime collectTime = parseForecastTimeStr(forecastTimeStr);
        String stationId = stationService.getStationIdByCode(stationCode);
        
        return PowerForecastData.builder()
                .collectTime(collectTime)
                .forecastTime(collectTime) // 初始设置为相同时间，后续根据具体数据类型调整
                .stationCode(stationCode)
                .indexCode(indicatorType.getValue())
                .energyType(energyType)
                .assetCode(stationId)
                .forecastValue(value)
                .orderNo(orderNo)
                .filePath(filePath)
                .fileName(filename)
                .createTime(LocalDateTime.now())
                .build();
    }

    private static String getEntityTime(String line) {
        Pattern timePattern = Pattern.compile("time='([\\d-_:]+)'");
        Matcher matcher = timePattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("无法解析时间字符串: " + line);
    }
}
