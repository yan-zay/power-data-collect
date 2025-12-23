package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
public class SftpFileParser {

    /**
     * 从远程 SFTP 读取并解析预测数据文件（适配两列格式）
     */
    public PowerForecastData parseForecastFileFromSftp(IndicatorTypeEnum indicatorType, InputStream in, String filePath, String filename) {
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

            return PowerForecastData.builder()
                    .stationCode(stationCode)
                    .indicatorType(indicatorType.getValue())
                    .forecastTimeStr(forecastTimeStr != null ? forecastTimeStr : "2025-12-18 00:00:00")
                    .forecastTime(parseForecastTimeStr(forecastTimeStr))
                    .filePath(filePath)
                    .fileName(filename)
                    .createTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .forecastData(dataLines)
                    .build();

        } catch (Exception e) {
            log.error("❌ 读取或解析文件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private static String getEntityTime(String line) {
        Pattern timePattern = Pattern.compile("time='([\\d-_:]+)'");
        Matcher matcher = timePattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
/*            // 根据字符串长度判断是哪种格式
            if (timeStr.length() == 16) {  // "yyyy-MM-dd_HH:mm" 格式
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm");
                return LocalDateTime.parse(timeStr, formatter);
            } else if (timeStr.length() == 19) {  // "yyyy-MM-dd_HH:mm:ss" 格式
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
                return LocalDateTime.parse(timeStr, formatter);
            }*/
        }
        throw new IllegalArgumentException("无法解析时间字符串: " + line);
    }


    /**
     * 根据指标类型统一解析日期格式为 "yyyy-MM-dd_HH:mm:ss"
     * @param forecastTimeStr 原始时间字符串
     * @return 格式化后的时间字符串
     */
    public static String parseForecastTimeStr(String forecastTimeStr) {
        if (forecastTimeStr == null || forecastTimeStr.isEmpty()) {
            return forecastTimeStr;
        }
        String normalizedTimeStr = forecastTimeStr.replace("_", " ");

        // 根据字符串长度判断是哪种格式
        if (normalizedTimeStr.length() == 16) {  // "yyyy-MM-dd_HH:mm" 格式 (CDQ类型)
            return normalizedTimeStr + ":00"; // 补充秒数 ":00"
        } else if (normalizedTimeStr.length() == 19) {  // "yyyy-MM-dd_HH:mm:ss" 格式 (DQ类型)
            return normalizedTimeStr; // 已经是标准格式
        }

        return normalizedTimeStr; // 其他情况保持原样
    }

    public static String getPathPart(String filePath, int part) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        String[] parts = filePath.split("/");
        if (parts.length >= part + 1) { // 路径以/开头会产生一个空的第一项
            return parts[part]; // 第五项实际上是第四级目录
        }
        throw new IllegalArgumentException("Invalid file path:{}" + filePath + ", part{}" + part);
    }
}
