package com.dtxytech.powerdatacollect.controller;

/**
 * @Author zay
 * @Date 2025/12/16 17:26
 */
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SftpForecastParser {

    /**
     * 从远程 SFTP 读取并解析预测数据文件（适配两列格式）
     */
    public static PowerForecastData parseForecastFileFromSftp(
            String host,
            int port,
            String username,
            String password,
            String remotePath,
            String fileName) {

        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            // === 1. 建立 SFTP 连接 ===
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(5000);

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect(5000);

            // === 2. 流式读取并解析 ===
            try (InputStream in = sftpChannel.get(remotePath);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

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
                String stationId = fileName.contains("_") ? fileName.substring(0, fileName.indexOf('_')) : "UNKNOWN";

                return PowerForecastData.builder()
                        .stationId(stationId)
                        .indicatorType(indicatorType)
                        .forecastTime(entityTime != null ? entityTime : "")
                        .fileName(fileName)
                        .forecastData(dataLines)
                        .build();

            } catch (Exception e) {
                System.err.println("❌ 读取或解析文件失败: " + e.getMessage());
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ SFTP 连接失败: " + e.getMessage());
            return null;
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) sftpChannel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    // ===== 数据模型 =====
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PowerForecastData {
        private String stationId;       // 如 "DTDL4"
        private String indicatorType;   // 如 "DTCG"
        private String forecastTime;    // 如 "2025-12-13 16:30"
        private String fileName;        // 原始文件名
        private List<String> forecastData; // 每项如 "#1\t8.05"
    }

    // ===== 测试用例 =====
    public static void main(String[] args) {
        // 模拟从 SFTP 读取（这里你可以替换为真实参数）
        PowerForecastData result = parseForecastFileFromSftp(
                "172.30.1.25",
                22,
                "sftp",
                "JCDZ@sp.0",
                "/home/ies/success/CF_Cg/20251216/DTCG__202512152130_CDQ.WPD",
                "DTCG__202512152130_CDQ.WPD"
        );

        if (result != null) {
            System.out.println("✅ 解析成功!");
            System.out.println("Station ID: " + result.getStationId());
            System.out.println("Indicator Type: " + result.getIndicatorType());
            System.out.println("Forecast Time: " + result.getForecastTime());
            System.out.println("Data Lines (" + result.getForecastData().size() + "):");
            result.getForecastData().forEach(System.out::println);
        } else {
            System.out.println("❌ 解析失败");
        }
    }
}
