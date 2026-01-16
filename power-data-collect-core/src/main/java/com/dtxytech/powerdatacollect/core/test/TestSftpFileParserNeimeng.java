package com.dtxytech.powerdatacollect.core.test;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.service.sftp.SftpFileParser;
import com.dtxytech.powerdatacollect.core.service.sftp.SftpFileParserNeimeng;
import com.dtxytech.powerdatacollect.core.service.station.StationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 测试内蒙古地区文件解析功能
 * 解析 DTXSF_20251215_DQ.WPD 和 DTXSF_202512142115_CDQ.WPD 文件
 */
public class TestSftpFileParserNeimeng {

    public static void main(String[] args) {
        StationService mockStationService = new MockStationService();

        SftpFileParser parser = new SftpFileParserNeimeng(mockStationService);

        // 测试解析DQ文件
        testDQFile(parser);

        // 测试解析CDQ文件
        testCDQFile(parser);
    }

    private static void testDQFile(SftpFileParser parser) {
        String dqFilePath = "D:\\WorkSpace\\DTXY\\power-data-collect\\power-data-collect-starter-boot\\src\\main\\resources\\sftp\\neimeng\\DTXSF_20251215_DQ.WPD";
        String filename = "DTXSF_20251215_DQ.WPD";

        // 创建临时文件并获取其路径
        Path tempPath = copyFileToTempDirectory(dqFilePath, "neimeng", filename);

        if (tempPath != null) {
            try {
                List<PowerForecastData> results = parser.parseFile(null, null, tempPath.toString());

                if (results != null) {
                    System.out.println("DQ文件解析结果数量: " + results.size());

                    // 打印前10条记录的详细信息
                    for (int i = 0; i < Math.min(10, results.size()); i++) {
                        PowerForecastData data = results.get(i);
                        System.out.println("第" + (i+1) + "条记录: collectTime=" + data.getCollectTime() +
                                ", forecastTime=" + data.getForecastTime() +
                                ", stationCode=" + data.getStationCode() +
                                ", indexCode=" + data.getIndexCode() +
                                ", energyType=" + data.getEnergyType() +
                                ", forecastValue=" + data.getForecastValue() +
                                ", orderNo=" + data.getOrderNo());
                    }

                    // 如果记录较多，打印最后几条
                    if (results.size() > 10) {
                        System.out.println("...");
                        int startIndex = Math.max(10, results.size() - 5);
                        for (int i = startIndex; i < results.size(); i++) {
                            PowerForecastData data = results.get(i);
                            System.out.println("第" + (i+1) + "条记录: collectTime=" + data.getCollectTime() +
                                    ", forecastTime=" + data.getForecastTime() +
                                    ", stationCode=" + data.getStationCode() +
                                    ", indexCode=" + data.getIndexCode() +
                                    ", energyType=" + data.getEnergyType() +
                                    ", forecastValue=" + data.getForecastValue() +
                                    ", orderNo=" + data.getOrderNo());
                        }
                    }
                } else {
                    System.out.println("DQ文件解析失败");
                }
            } finally {
                // 清理临时文件
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException e) {
                    System.out.println("删除临时文件失败: " + e.getMessage());
                }
            }
        } else {
            System.out.println("无法创建临时DQ文件");
        }
    }

    private static void testCDQFile(SftpFileParser parser) {
        String cdqFilePath = "D:\\WorkSpace\\DTXY\\power-data-collect\\power-data-collect-starter-boot\\src\\main\\resources\\sftp\\neimeng\\DTXSF_202512142115_CDQ.WPD";
        String filename = "DTXSF_202512142115_CDQ.WPD";

        // 创建临时文件并获取其路径
        Path tempPath = copyFileToTempDirectory(cdqFilePath, "neimeng", filename);

        if (tempPath != null) {
            try {
                List<PowerForecastData> results = parser.parseFile(null, null, tempPath.toString());

                if (results != null) {
                    System.out.println("CDQ文件解析结果数量: " + results.size());

                    // 打印所有记录的详细信息
                    for (int i = 0; i < results.size(); i++) {
                        PowerForecastData data = results.get(i);
                        System.out.println("第" + (i+1) + "条记录: collectTime=" + data.getCollectTime() +
                                ", forecastTime=" + data.getForecastTime() +
                                ", stationCode=" + data.getStationCode() +
                                ", indexCode=" + data.getIndexCode() +
                                ", energyType=" + data.getEnergyType() +
                                ", forecastValue=" + data.getForecastValue() +
                                ", orderNo=" + data.getOrderNo());
                    }
                } else {
                    System.out.println("CDQ文件解析失败");
                }
            } finally {
                // 清理临时文件
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException e) {
                    System.out.println("删除临时文件失败: " + e.getMessage());
                }
            }
        } else {
            System.out.println("无法创建临时CDQ文件");
        }
    }

    /**
     * 将文件复制到临时目录，并构建合适的路径结构
     */
    private static Path copyFileToTempDirectory(String sourceFilePath, String region, String filename) {
        try {
            // 获取系统临时目录
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), region);
            Path targetPath = tempDir.resolve(filename);

            // 确保目录存在
            Files.createDirectories(tempDir);

            // 复制文件
            Path sourcePath = Paths.get(sourceFilePath);
            Files.copy(sourcePath, targetPath);

            return targetPath;
        } catch (IOException e) {
            System.out.println("复制文件到临时目录失败: " + e.getMessage());
            return null;
        }
    }

    // Mock StationService 实现
    static class MockStationService implements StationService {
        @Override
        public String getStationIdByCode(String stationCode) {
            // 简单返回与stationCode相同的值，实际使用时会被Spring注入真实实现
            return stationCode;
        }
    }
}
