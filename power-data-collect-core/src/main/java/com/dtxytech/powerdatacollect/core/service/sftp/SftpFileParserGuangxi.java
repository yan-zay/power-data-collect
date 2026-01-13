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
@ConditionalOnProperty(name = "sftp.region", havingValue = "guangxi", matchIfMissing = false)
public class SftpFileParserGuangxi extends SftpFileParser {

    private StationService stationService;

    /**
     * 从远程 SFTP 读取并解析预测数据文件（广西地区特殊格式）
     * 解析包含 CDQYC 和 DQYC 后缀的文件
     */
    @Override
    public List<PowerForecastData> parseForecastFileFromSftp(IndicatorTypeEnum indicatorType, InputStream in,
                                                             String filePath, String filename) {
        // === 2. 流式读取并解析 ===
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            String line;
            String forecastTimeStr = null;
            String stationCode = null;
            String dateStr = null;
            String timeStr = null;
            List<PowerForecastData> result = new ArrayList<>();

            // 从文件名提取站点代码（例如：FD_GX.FengPDC_CDQYC_20251201_000000.dat -> FengPDC）
            stationCode = extractStationCodeFromFile(filename);
            
            boolean insideDataBlock = false;
            String currentBlockType = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) continue;

                // 跳过注释行
                if (line.startsWith("//")) {
                    continue;
                }

                // 解析包含 Date 和 Time 的标签行，如 <DQYC::GX.FengPDC Date='2025-12-20' Time='00-15-00'>
                if ((line.startsWith("<DQYC::") || line.startsWith("<CDQYC::")) && line.contains("Date=") && line.contains("Time=")) {
                    // 提取站点代码（如果还没有提取的话）
                    if (stationCode == null || stationCode.isEmpty()) {
                        stationCode = extractStationCodeFromLine(line);
                    }
                    
                    // 提取 Date 和 Time
                    String[] parts = line.split("\\s+");
                    for (String part : parts) {
                        if (part.contains("Date='")) {
                            dateStr = extractValue(part, "Date='", "'");
                        } else if (part.contains("Time='")) {
                            timeStr = extractValue(part, "Time='", "'");
                        }
                    }
                    if (dateStr != null && timeStr != null) {
                        forecastTimeStr = dateStr + "_" + timeStr.replace("-", ":");
                    }
                    
                    // 确定当前数据块类型
                    if (line.startsWith("<DQYC::")) {
                        currentBlockType = "DQYC";
                    } else if (line.startsWith("<CDQYC::")) {
                        currentBlockType = "CDQYC";
                    }
                    
                    // 检查是否应该处理此数据块
                    if ((indicatorType == IndicatorTypeEnum.DQ && currentBlockType.equals("DQYC")) ||
                        (indicatorType == IndicatorTypeEnum.CDQ && currentBlockType.equals("CDQYC"))) {
                        insideDataBlock = true;
                    }
                    continue;
                }

                // 如果在正确的数据块内，解析数据
                if (insideDataBlock) {
                    if (line.startsWith("</DQYC::") || line.startsWith("</CDQYC::")) {
                        // 结束当前数据块
                        insideDataBlock = false;
                        currentBlockType = null;
                        continue;
                    }
                    
                    // 解析标题行，找到"功率预测"列的索引
                    if (line.startsWith("@")) {
                        continue; // 已经在处理数据块内部，跳过标题行
                    }
                    
                    // 解析数据行
                    if (line.startsWith("#")) {
                        if (currentBlockType != null) {
                            if (currentBlockType.equals("DQYC") && indicatorType == IndicatorTypeEnum.DQ) {
                                // 解析DQYC数据
                                result.addAll(parseSingleDQYCLine(line, indicatorType, filePath, filename, 
                                                                stationCode, forecastTimeStr, result.size() + 1));
                            } else if (currentBlockType.equals("CDQYC") && indicatorType == IndicatorTypeEnum.CDQ) {
                                // 解析CDQYC数据
                                result.addAll(parseSingleCDQYCLine(line, indicatorType, filePath, filename, 
                                                                 stationCode, forecastTimeStr, result.size() + 1));
                            }
                        }
                    }
                }
            }

            log.info("parseForecastFileFromSftp stationCode:{}, result size:{}", stationCode, result.size());

            return result;
        } catch (Exception e) {
            log.error("❌ 读取或解析文件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析单行DQYC数据
     */
    private List<PowerForecastData> parseSingleDQYCLine(String line, IndicatorTypeEnum indicatorType, 
                                                      String filePath, String filename, String stationCode, 
                                                      String forecastTimeStr, int orderNo) {
        List<PowerForecastData> result = new ArrayList<>();
        String[] parts = line.substring(1).trim().split("\\s+"); // 移除开头的#号
        
        // 标题行通常为 "@	时间顺序列	功率预测	计划开机容量	运行台数"
        // 数据行格式为 "#	序号	功率预测值	计划开机容量	运行台数"
        // 所以功率预测值在第2个位置（索引1）
        if (parts.length >= 2) {
            String valueStr = parts[1]; // 第二列是功率预测值
            
            try {
                BigDecimal value = new BigDecimal(valueStr);
                
                PowerForecastData obj = buildPowerForecastData(
                    indicatorType, filePath, filename, stationCode, 
                    forecastTimeStr, value, orderNo, getEnergyTypeFromFile(filename));
                    
                LocalDateTime baseForecastTime = parseForecastTimeStr(forecastTimeStr);
                // 设置forecastTime为baseForecastTime加上相应的分钟数
                // 对于DQYC数据，每行代表15分钟间隔
                obj.setForecastTime(baseForecastTime.plusMinutes((orderNo - 1) * 15L));
                    
                result.add(obj);
            } catch (NumberFormatException e) {
                log.warn("无法解析短期预测功率数据: {}", line);
            }
        }
        
        return result;
    }

    /**
     * 解析单行CDQYC数据
     */
    private List<PowerForecastData> parseSingleCDQYCLine(String line, IndicatorTypeEnum indicatorType, 
                                                       String filePath, String filename, String stationCode, 
                                                       String forecastTimeStr, int orderNo) {
        List<PowerForecastData> result = new ArrayList<>();
        String[] parts = line.substring(1).trim().split("\\s+"); // 移除开头的#号
        
        // 标题行通常为 "@	时间顺序列	功率预测	当前正在运行的机组总容量	功率预测<可用>"
        // 数据行格式为 "#	序号	功率预测值	当前正在运行的机组总容量	功率预测<可用>"
        // 所以功率预测值在第2个位置（索引1）
        if (parts.length >= 2) {
            String valueStr = parts[1]; // 第二列是功率预测值
            
            try {
                BigDecimal value = new BigDecimal(valueStr);
                
                PowerForecastData obj = buildPowerForecastData(
                    indicatorType, filePath, filename, stationCode, 
                    forecastTimeStr, value, orderNo, getEnergyTypeFromFile(filename));
                    
                LocalDateTime baseForecastTime = parseForecastTimeStr(forecastTimeStr);
                // 设置forecastTime为baseForecastTime加上相应的分钟数
                // 对于CDQYC数据，每行代表15分钟间隔
                obj.setForecastTime(baseForecastTime.plusMinutes((orderNo - 1) * 15L));
                    
                result.add(obj);
            } catch (NumberFormatException e) {
                log.warn("无法解析超短期预测功率数据: {}", line);
            }
        }
        
        return result;
    }

    /**
     * 从文件名提取站点代码
     */
    private String extractStationCodeFromFile(String filename) {
        // 文件名格式如: FD_GX.FengPDC_CDQYC_20251201_000000.dat
        if (filename != null && filename.contains(".")) {
            // 提取 FD_GX.FengPDC_CDQYC 部分
            String prefix = filename.substring(0, filename.lastIndexOf('_'));
            // 提取 FengPDC 部分
            String[] parts = prefix.split("\\.");
            if (parts.length >= 3) {
                return parts[2]; // 返回第三部分，如 FengPDC
            }
        }
        return "";
    }

    /**
     * 从标签行中提取站点代码，例如从 <DQYC::GX.FengPDC Date='2025-12-20' Time='00-15-00'> 中提取 FengPDC
     */
    private String extractStationCodeFromLine(String line) {
        // 匹配 <DQYC::GX.FengPDC 或 <CDQYC::GX.FengPDC 格式
        Pattern pattern = Pattern.compile("<(?:DQYC|CDQYC)::[A-Z]+\\.([A-Za-z0-9]+)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1); // 返回站点代码
        }
        return "";
    }

    /**
     * 提取字符串中的值
     */
    private String extractValue(String line, String startMarker, String endMarker) {
        int startIndex = line.indexOf(startMarker);
        if (startIndex != -1) {
            startIndex += startMarker.length();
            int endIndex = line.indexOf(endMarker, startIndex);
            if (endIndex != -1) {
                return line.substring(startIndex, endIndex);
            }
        }
        return null;
    }

    /**
     * 解析短期预测功率数据块 (DQ - DQYC)
     * 此方法不再使用，因为我们直接在主循环中处理每一行
     */
    @Deprecated
    private List<PowerForecastData> parseDQYCBlock(BufferedReader reader, IndicatorTypeEnum indicatorType, 
                                                            String filePath, String filename, String stationCode, 
                                                            String forecastTimeStr) throws java.io.IOException {
        // 这个方法已废弃，实际解析逻辑已在parseForecastFileFromSftp方法中实现
        return new ArrayList<>();
    }
    
    /**
     * 处理DQYC数据行
     * 此方法不再使用
     */
    @Deprecated
    private void processDQYCDataLine(String line, List<PowerForecastData> result, IndicatorTypeEnum indicatorType,
                                   String filePath, String filename, String stationCode, 
                                   String forecastTimeStr, LocalDateTime baseForecastTime, int orderNo, int powerColumnIndex) {
        // 这个方法已废弃
    }

    /**
     * 解析超短期预测功率数据块 (CDQ - CDQYC)
     * 此方法不再使用，因为我们直接在主循环中处理每一行
     */
    @Deprecated
    private List<PowerForecastData> parseCDQYCBlock(BufferedReader reader, IndicatorTypeEnum indicatorType, 
                                                                 String filePath, String filename, String stationCode, 
                                                                 String forecastTimeStr) throws java.io.IOException {
        // 这个方法已废弃，实际解析逻辑已在parseForecastFileFromSftp方法中实现
        return new ArrayList<>();
    }
    
    /**
     * 处理CDQYC数据行
     * 此方法不再使用
     */
    @Deprecated
    private void processCDQYCDataLine(String line, List<PowerForecastData> result, IndicatorTypeEnum indicatorType,
                                    String filePath, String filename, String stationCode, 
                                    String forecastTimeStr, LocalDateTime baseForecastTime, int orderNo, int powerColumnIndex) {
        // 这个方法已废弃
    }

    /**
     * 根据文件名前缀确定energyType
     */
    private String getEnergyTypeFromFile(String filename) {
        if (filename != null) {
            filename = filename.toUpperCase();
            if (filename.startsWith("FD_")) {
                return "wind"; // FD开头代表风电
            } else if (filename.startsWith("GF_")) {
                return "pv"; // GF开头代表光伏
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

    /**
     * 根据指标类型统一解析日期格式为 LocalDateTime
     * @param forecastTimeStr 原始时间字符串
     * @return 解析后的 LocalDateTime 对象
     */
    public LocalDateTime parseForecastTimeStr(String forecastTimeStr) {
        if (forecastTimeStr == null || forecastTimeStr.isEmpty()) {
            log.error("forecastTimeStr is null or empty!");
            return null;
        }
        
        // 格式为 "yyyy-MM-dd_HH:mm:ss" 或 "yyyy-MM-dd_HH:mm" 或 "yyyy-MM-dd_HH-mm-ss" (需要转换为标准格式)
        String normalizedTimeStr = forecastTimeStr.replace("-", ":").replace("_", " ");
        // 处理类似 "2025:12:01 00:15:00" 的格式
        String[] parts = normalizedTimeStr.split(" ");
        if (parts.length == 2) {
            String datePart = parts[0].replace(":", "-"); // 转换回日期格式
            String timePart = parts[1];
            normalizedTimeStr = datePart + " " + timePart;
        }

        // 根据字符串长度判断是哪种格式
        try {
            return LocalDateTime.parse(normalizedTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            log.error("无法解析时间字符串: {}", normalizedTimeStr, e);
            return null;
        }
    }
}
