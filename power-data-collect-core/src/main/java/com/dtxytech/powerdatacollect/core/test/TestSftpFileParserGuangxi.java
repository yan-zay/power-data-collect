package com.dtxytech.powerdatacollect.core.test;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.service.sftp.SftpFileParser;
import com.dtxytech.powerdatacollect.core.service.sftp.SftpFileParserGuangxi;
import com.dtxytech.powerdatacollect.core.service.station.StationService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 测试 SftpFileParserGuangxi 解析功能
 */
@Slf4j
public class TestSftpFileParserGuangxi {

    public static void main(String[] args) {
        StationService mockStationService = new MockStationService();

        SftpFileParser parser = new SftpFileParserGuangxi(mockStationService);

        // 测试解析DQ文件
        testDQFile(parser);

        // 测试解析CDQ文件
        testCDQFile(parser);
    }

    private static void testDQFile(SftpFileParser parser) {
        String dqFilePath = "D:\\WorkSpace\\DTXY\\power-data-collect\\power-data-collect-starter-boot\\src\\main\\resources\\sftp\\guangxi\\FD_GX.FengPDC_DQYC_20251219_083000.dat";
        String filename = "FD_GX.FengPDC_DQYC_20251219_083000.dat";

        // 创建临时文件并获取其路径
        Path tempPath = copyFileToTempDirectory(dqFilePath, "guangxi", filename);

        if (tempPath != null) {
            try {
                List<PowerForecastData> results = parser.parseFile(tempPath.toString());

                if (results != null) {
                    log.info("DQ文件解析结果数量: {}", results.size());

                    // 打印前10条记录的详细信息
                    for (int i = 0; i < Math.min(10, results.size()); i++) {
                        PowerForecastData data = results.get(i);
                        log.info("第{}条记录: data={}", data.getOrderNo(), data);
                    }
                } else {
                    log.error("DQ文件解析失败");
                }
            } finally {
                // 清理临时文件
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException e) {
                    log.warn("删除临时文件失败: {}", e.getMessage());
                }
            }
        } else {
            log.error("无法创建临时DQ文件");
        }
    }

    private static void testCDQFile(SftpFileParser parser) {
        String cdqFilePath = "D:\\WorkSpace\\DTXY\\power-data-collect\\power-data-collect-starter-boot\\src\\main\\resources\\sftp\\guangxi\\FD_GX.FengPDC_CDQYC_20251201_000000.dat";
        String filename = "FD_GX.FengPDC_CDQYC_20251201_000000.dat";

        // 创建临时文件并获取其路径
        Path tempPath = copyFileToTempDirectory(cdqFilePath, "guangxi", filename);

        if (tempPath != null) {
            try {
                List<PowerForecastData> results = parser.parseFile(tempPath.toString());

                if (results != null) {
                    log.info("CDQ文件解析结果数量: {}", results.size());

                    // 打印前10条记录的详细信息
                    for (int i = 0; i < Math.min(10, results.size()); i++) {
                        PowerForecastData data = results.get(i);
                        log.info("第{}条记录: collectTime={}, forecastTime={}, stationCode={}, indexCode={}, energyType={}, forecastValue={}, orderNo={}",
                            data.getOrderNo(), data.getCollectTime(), data.getForecastTime(),
                            data.getStationCode(), data.getIndexCode(), data.getEnergyType(),
                            data.getForecastValue(), data.getOrderNo());
                    }
                } else {
                    log.error("CDQ文件解析失败");
                }
            } finally {
                // 清理临时文件
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException e) {
                    log.warn("删除临时文件失败: {}", e.getMessage());
                }
            }
        } else {
            log.error("无法创建临时CDQ文件");
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
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            return targetPath;
        } catch (IOException e) {
            log.error("复制文件到临时目录失败: {}", e.getMessage());
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
