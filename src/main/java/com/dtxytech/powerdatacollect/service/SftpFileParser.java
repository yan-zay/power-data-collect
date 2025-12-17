package com.dtxytech.powerdatacollect.service;

import com.dtxytech.powerdatacollect.entity.PowerForecastData;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    public PowerForecastData parseForecastFileFromSftp(String remoteFilePath, InputStream in) {
        // === 2. 流式读取并解析 ===
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            String line;
            boolean inDataBlock = false;
            List<String> dataLines = new ArrayList<>();
            String entityTime = null;
            String indicatorType = "DTCG"; // 默认，也可从 type=... 提取

            Pattern entityPattern = Pattern.compile("<!\\s*Entity=forecastdata.*type=([A-Z]+).*time='([^']+)'");

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) continue;

                // 跳过注释行
                if (line.startsWith("//")) {
                    continue;
                }

                // 解析 Entity 行，提取 type 和 time
                if (line.startsWith("<!") && line.contains("Entity=forecastdata")) {
                    Matcher m = entityPattern.matcher(line);
                    if (m.find()) {
                        indicatorType = m.group(1); // 如 DTCG
                        entityTime = m.group(2).replace('_', ' '); // "2025-12-13_16:30" → "2025-12-13 16:30"
                    }
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
            String stationId = remoteFilePath.contains("_") ? remoteFilePath.substring(0, remoteFilePath.indexOf('_')) : "UNKNOWN";

            return PowerForecastData.builder()
                    .stationId(stationId)
                    .indicatorType(indicatorType)
                    .forecastTime(entityTime != null ? entityTime : "")
                    .fileName(remoteFilePath)
                    .forecastData(dataLines)
                    .build();

        } catch (Exception e) {
            System.err.println("❌ 读取或解析文件失败: " + e.getMessage());
            return null;
        }
    }
}
