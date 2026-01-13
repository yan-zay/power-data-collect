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
@ConditionalOnProperty(name = "sftp.region", havingValue = "longjiang", matchIfMissing = false)
public class SftpFileParserLongjiang extends SftpFileParser {

    private StationService stationService;

    /**
     * 从远程 SFTP 读取并解析预测数据文件（黑龙江地区特殊格式）
     * 解析包含 DQ 和 CDQ 后缀的文件
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

                // 处理不同的数据块类型
                if (line.contains("<RunningCapacity::")) {
                    result.addAll(parseRunningCapacityBlock(reader, indicatorType, filePath, filename, stationCode, forecastTimeStr));
                } else if (line.contains("<UltraShortTermForcast_P2P::")) {
                    result.addAll(parseUltraShortTermForcastBlock(reader, indicatorType, filePath, filename, stationCode, forecastTimeStr, "P2P"));
                } else if (line.contains("<UltraShortTermForcast_V2P::")) {
                    result.addAll(parseUltraShortTermForcastBlock(reader, indicatorType, filePath, filename, stationCode, forecastTimeStr, "V2P"));
                } else if (line.contains("<UltraShortTermForcast_TP::")) {
                    result.addAll(parseUltraShortTermForcastBlock(reader, indicatorType, filePath, filename, stationCode, forecastTimeStr, "TP"));
                } else if (line.contains("<UltraShortTermForcast_AP::")) {
                    result.addAll(parseUltraShortTermForcastBlock(reader, indicatorType, filePath, filename, stationCode, forecastTimeStr, "AP"));
                } else if (line.contains("<ShortTermForcast::")) {
                    result.addAll(parseShortTermForcastBlock(reader, indicatorType, filePath, filename, stationCode, forecastTimeStr));
                } else if (line.contains("<ShortTermForcast_AP::")) {
                    result.addAll(parseShortTermForcastAPBlock(reader, indicatorType, filePath, filename, stationCode, forecastTimeStr));
                } else if (line.contains("<Capacity::")) {
                    result.addAll(parseCapacityBlock(reader, indicatorType, filePath, filename, stationCode, forecastTimeStr));
                } else if (line.contains("<NWPDATA::")) {
                    result.addAll(parseNWPDataBlock(reader, indicatorType, filePath, filename, stationCode, forecastTimeStr));
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
     * 解析实时开机容量数据块
     */
    private List<PowerForecastData> parseRunningCapacityBlock(BufferedReader reader, IndicatorTypeEnum indicatorType, 
                                                              String filePath, String filename, String stationCode, 
                                                              String forecastTimeStr) throws java.io.IOException {
        List<PowerForecastData> result = new ArrayList<>();
        String line;
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            
            if (line.equals("</RunningCapacity::" + stationCode + ">")) {
                break; // 结束当前数据块
            }
            
            if (line.startsWith("#")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    String orderNoStr = parts[0].substring(1); // 去掉#号
                    String valueStr = parts[1];
                    
                    try {
                        BigDecimal value = new BigDecimal(valueStr);
                        Integer orderNo = Integer.parseInt(orderNoStr);
                        
                        PowerForecastData obj = buildPowerForecastData(
                            indicatorType, filePath, filename, stationCode, 
                            forecastTimeStr, value, orderNo, "RunningCapacity");
                            
                        result.add(obj);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析实时开机容量数据: {}", line);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 解析超短期预测功率数据块
     */
    private List<PowerForecastData> parseUltraShortTermForcastBlock(BufferedReader reader, IndicatorTypeEnum indicatorType, 
                                                                 String filePath, String filename, String stationCode, 
                                                                 String forecastTimeStr, String forecastType) throws java.io.IOException {
        List<PowerForecastData> result = new ArrayList<>();
        String line;
        LocalDateTime baseForecastTime = parseForecastTimeStr(forecastTimeStr);
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            
            if (line.contains("</UltraShortTermForcast_" + forecastType + "::" + stationCode + ">")) {
                break; // 结束当前数据块
            }
            
            if (line.startsWith("#")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    String orderNoStr = parts[0].substring(1); // 去掉#号
                    String valueStr = parts[1];
                    
                    try {
                        BigDecimal value = new BigDecimal(valueStr);
                        Integer orderNo = Integer.parseInt(orderNoStr);
                        
                        PowerForecastData obj = buildPowerForecastData(
                            indicatorType, filePath, filename, stationCode, 
                            forecastTimeStr, value, orderNo, "UltraShortTermForcast_" + forecastType);
                            
                        // 超短期预测通常是未来15分钟间隔的数据
                        obj.setForecastTime(baseForecastTime.plusMinutes((orderNo - 1) * 15));
                            
                        result.add(obj);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析超短期预测功率数据: {}", line);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 解析短期预测功率数据块
     */
    private List<PowerForecastData> parseShortTermForcastBlock(BufferedReader reader, IndicatorTypeEnum indicatorType, 
                                                            String filePath, String filename, String stationCode, 
                                                            String forecastTimeStr) throws java.io.IOException {
        List<PowerForecastData> result = new ArrayList<>();
        String line;
        LocalDateTime baseForecastTime = parseForecastTimeStr(forecastTimeStr);
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            
            if (line.equals("</ShortTermForcast::" + stationCode + ">")) {
                break; // 结束当前数据块
            }
            
            if (line.startsWith("#")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) { // 至少包含序号、预测值和装机容量
                    String orderNoStr = parts[0].substring(1); // 去掉#号
                    String valueStr = parts[1];
                    
                    try {
                        BigDecimal value = new BigDecimal(valueStr);
                        Integer orderNo = Integer.parseInt(orderNoStr);
                        
                        PowerForecastData obj = buildPowerForecastData(
                            indicatorType, filePath, filename, stationCode, 
                            forecastTimeStr, value, orderNo, "ShortTermForcast");
                            
                        // 短期预测通常是未来15分钟间隔的数据
                        obj.setForecastTime(baseForecastTime.plusHours(orderNo - 1));
                            
                        result.add(obj);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析短期预测功率数据: {}", line);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 解析短期预测可用功率数据块
     */
    private List<PowerForecastData> parseShortTermForcastAPBlock(BufferedReader reader, IndicatorTypeEnum indicatorType, 
                                                              String filePath, String filename, String stationCode, 
                                                              String forecastTimeStr) throws java.io.IOException {
        List<PowerForecastData> result = new ArrayList<>();
        String line;
        LocalDateTime baseForecastTime = parseForecastTimeStr(forecastTimeStr);
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            
            if (line.equals("</ShortTermForcast_AP::" + stationCode + ">")) {
                break; // 结束当前数据块
            }
            
            if (line.startsWith("#")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    String orderNoStr = parts[0].substring(1); // 去掉#号
                    String valueStr = parts[1];
                    
                    try {
                        BigDecimal value = new BigDecimal(valueStr);
                        Integer orderNo = Integer.parseInt(orderNoStr);
                        
                        PowerForecastData obj = buildPowerForecastData(
                            indicatorType, filePath, filename, stationCode, 
                            forecastTimeStr, value, orderNo, "ShortTermForcast_AP");
                            
                        // 短期预测通常是未来15分钟间隔的数据
                        obj.setForecastTime(baseForecastTime.plusHours(orderNo - 1));
                            
                        result.add(obj);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析短期预测可用功率数据: {}", line);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 解析装机容量数据块
     */
    private List<PowerForecastData> parseCapacityBlock(BufferedReader reader, IndicatorTypeEnum indicatorType, 
                                                    String filePath, String filename, String stationCode, 
                                                    String forecastTimeStr) throws java.io.IOException {
        List<PowerForecastData> result = new ArrayList<>();
        String line;
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            
            if (line.equals("</Capacity::" + stationCode + ">")) {
                break; // 结束当前数据块
            }
            
            if (line.startsWith("#")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    String orderNoStr = parts[0].substring(1); // 去掉#号
                    String valueStr = parts[1];
                    
                    try {
                        BigDecimal value = new BigDecimal(valueStr);
                        Integer orderNo = Integer.parseInt(orderNoStr);
                        
                        PowerForecastData obj = buildPowerForecastData(
                            indicatorType, filePath, filename, stationCode, 
                            forecastTimeStr, value, orderNo, "Capacity");
                            
                        result.add(obj);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析装机容量数据: {}", line);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 解析数值天气预报数据块
     */
    private List<PowerForecastData> parseNWPDataBlock(BufferedReader reader, IndicatorTypeEnum indicatorType,
                                                   String filePath, String filename, String stationCode, 
                                                   String forecastTimeStr) throws java.io.IOException {
        List<PowerForecastData> result = new ArrayList<>();
        String line;
        LocalDateTime baseForecastTime = parseForecastTimeStr(forecastTimeStr);
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            
            if (line.equals("</NWPDATA::" + stationCode + ">")) {
                break; // 结束当前数据块
            }
            
            if (line.startsWith("#")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    String orderNoStr = parts[0].substring(1); // 去掉#号
                    // 对于NWP数据，我们可以存储风速作为预测值
                    if (parts.length >= 4) { // 时间 高度 风速 方向
                        String windSpeedStr = parts[3];
                        
                        try {
                            BigDecimal value = new BigDecimal(windSpeedStr);
                            Integer orderNo = Integer.parseInt(orderNoStr);
                            
                            PowerForecastData obj = buildPowerForecastData(
                                indicatorType, filePath, filename, stationCode, 
                                forecastTimeStr, value, orderNo, "NWP_WindSpeed");
                                
                            // NWP数据通常是每小时的数据点
                            obj.setForecastTime(baseForecastTime.plusHours(orderNo - 1));
                                
                            result.add(obj);
                        } catch (NumberFormatException e) {
                            log.warn("无法解析NWP数据: {}", line);
                        }
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 构建PowerForecastData对象
     */
    private PowerForecastData buildPowerForecastData(IndicatorTypeEnum indicatorType, String filePath,
                                                     String filename, String stationCode, 
                                                     String forecastTimeStr, BigDecimal value, 
                                                     Integer orderNo, String dataType) {
        LocalDateTime collectTime = parseForecastTimeStr(forecastTimeStr);
        String stationId = stationService.getStationIdByCode(stationCode);
        
        return PowerForecastData.builder()
                .collectTime(collectTime)
                .forecastTime(collectTime) // 初始设置为相同时间，后续根据具体数据类型调整
                .stationCode(stationCode)
                .indexCode(indicatorType.getValue())
                .energyType(dataType)
                .assetCode(stationId)
                .forecastValue(value)
                .orderNo(orderNo)
                .filePath(filePath)
                .fileName(filename)
                .createTime(LocalDateTime.now())
                .build();
    }

    protected String getEntityTime(String line) {
        Pattern timePattern = Pattern.compile("time='([\\d-_:]+)'");
        Matcher matcher = timePattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("无法解析时间字符串: " + line);
    }
}
