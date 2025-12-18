package com.dtxytech.powerdatacollect.service;

import com.dtxytech.powerdatacollect.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.enums.IndicatorTypeEnum;
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
            String entityTime = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) continue;

                // 跳过注释行
                if (line.startsWith("//")) {
                    continue;
                }

                // 解析 Entity 行，提取 type 和 time
                if (line.startsWith("<!") && line.contains("time=")) {
                    entityTime = getEntityTime(line);
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
                    // 清理末尾可能的 tab 或空格
//                        dataLines.add(line);
                    String[] cols = line.split("\t");
                    if (cols.length >= 2) {
                        dataLines.add(cols[1]); // "0.65"
                    }
                }
            }

            // 推断 stationId：从 fileName 提取前缀（如 DTDL4_... → DTDL4）
            String stationCode = getStationCode(filePath);

            return PowerForecastData.builder()
                    .stationCode(stationCode)
                    .indicatorType(indicatorType.getValue())
                    .forecastTime(entityTime != null ? entityTime : "2025-12-18 00:00:00")
                    .filePath(filePath)
                    .fileName(filename)
                    .createTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .forecastData(dataLines)
                    .build();

        } catch (Exception e) {
            System.err.println("❌ 读取或解析文件失败: " + e.getMessage());
            return null;
        }
    }

    private static String getEntityTime(String line) {
        Pattern timePattern = Pattern.compile("time='([\\d-_:]+)'");
        Matcher matcher = timePattern.matcher(line);
        if (matcher.find()) {
            String timeStr = matcher.group(1);
            return timeStr;

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

    private static String getStationCode(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        String[] parts = filePath.split("/");
        if (parts.length >= 5) { // 路径以/开头会产生一个空的第一项
            return parts[4]; // 第五项实际上是第四级目录
        }

        return "";
    }
}
