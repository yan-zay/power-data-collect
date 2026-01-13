package com.dtxytech.powerdatacollect.core.test;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.service.sftp.SftpFileParserLongjiang;
import com.dtxytech.powerdatacollect.core.service.station.StationService;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * 测试黑龙江地区文件解析功能
 */
public class TestSftpFileParserLongjiang {

    public static void main(String[] args) {
        // 创建测试实例 - 需要提供StationService的实例
        // 由于StationService是Spring组件，我们使用mock或直接实例化
        // 但为了测试，我们可以创建一个简单的测试版本

        // 创建一个mock的StationService
        StationService mockStationService = new MockStationService();

        // 创建测试解析器
        SftpFileParserLongjiang parser = new SftpFileParserLongjiang(mockStationService);

        // 测试解析DQ文件
        testParseDQFile(parser);

        // 测试解析CDQ文件
        testParseCDQFile(parser);

        // 测试解析另一个CDQ文件
        testParseCDQFile2(parser);
    }

    /**
     * 测试解析DQ文件
     */
    private static void testParseDQFile(SftpFileParserLongjiang parser) {
        System.out.println("=== 测试解析DQ文件 ===");
        try {
            // 从资源目录读取测试文件
            InputStream inputStream = new FileInputStream("D:\\WorkSpace\\DTXY\\power-data-collect\\power-data-collect-starter-boot\\src\\main\\resources\\sftp\\longjiang\\FAFD70_20260108_0000_DQ.WPD");

            List<PowerForecastData> result = parser.parseForecastFileFromSftp(
                IndicatorTypeEnum.DQ,
                inputStream,
                "/sftp/longjiang",
                "FAFD70_20260108_0000_DQ.WPD"
            );

            if (result != null) {
                System.out.println("解析DQ文件成功，共解析 " + result.size() + " 条数据");

                // 打印前几条数据进行验证
                int count = Math.min(10, result.size());
                for (int i = 0; i < count; i++) {
                    PowerForecastData data = result.get(i);
                    System.out.println("数据 " + (i+1) + ": " +
                        "collectTime=" + data.getCollectTime() +
                        ", forecastTime=" + data.getForecastTime() +
                        ", stationCode=" + data.getStationCode() +
                        ", indexCode=" + data.getIndexCode() +
                        ", energyType=" + data.getEnergyType() +
                        ", forecastValue=" + data.getForecastValue() +
                        ", orderNo=" + data.getOrderNo());
                }

                if (result.size() > 10) {
                    System.out.println("... 还有 " + (result.size() - 10) + " 条数据");
                }
            } else {
                System.out.println("解析DQ文件失败");
            }

            inputStream.close();
        } catch (Exception e) {
            System.err.println("测试DQ文件解析时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试解析CDQ文件
     */
    private static void testParseCDQFile(SftpFileParserLongjiang parser) {
        System.out.println("\n=== 测试解析CDQ文件 (0030_CDQ) ===");
        try {
            // 从资源目录读取测试文件
            InputStream inputStream = new FileInputStream("D:\\WorkSpace\\DTXY\\power-data-collect\\power-data-collect-starter-boot\\src\\main\\resources\\sftp\\longjiang\\FAFD70_20260108_0030_CDQ.WPD");

            List<PowerForecastData> result = parser.parseForecastFileFromSftp(
                IndicatorTypeEnum.CDQ,
                inputStream,
                "/sftp/longjiang",
                "FAFD70_20260108_0030_CDQ.WPD"
            );

            if (result != null) {
                System.out.println("解析CDQ文件成功，共解析 " + result.size() + " 条数据");

                // 打印所有数据
                for (int i = 0; i < result.size(); i++) {
                    PowerForecastData data = result.get(i);
                    System.out.println("数据 " + (i+1) + ": " +
                        "collectTime=" + data.getCollectTime() +
                        ", forecastTime=" + data.getForecastTime() +
                        ", stationCode=" + data.getStationCode() +
                        ", indexCode=" + data.getIndexCode() +
                        ", energyType=" + data.getEnergyType() +
                        ", forecastValue=" + data.getForecastValue() +
                        ", orderNo=" + data.getOrderNo());
                }
            } else {
                System.out.println("解析CDQ文件失败");
            }

            inputStream.close();
        } catch (Exception e) {
            System.err.println("测试CDQ文件解析时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试解析另一个CDQ文件
     */
    private static void testParseCDQFile2(SftpFileParserLongjiang parser) {
        System.out.println("\n=== 测试解析CDQ文件 (0015_CDQ) ===");
        try {
            // 从资源目录读取测试文件
            InputStream inputStream = new FileInputStream("D:\\WorkSpace\\DTXY\\power-data-collect\\power-data-collect-starter-boot\\src\\main\\resources\\sftp\\longjiang\\FAFD70_20260108_0015_CDQ.WPD");

            List<PowerForecastData> result = parser.parseForecastFileFromSftp(
                IndicatorTypeEnum.CDQ,
                inputStream,
                "/sftp/longjiang",
                "FAFD70_20260108_0015_CDQ.WPD"
            );

            if (result != null) {
                System.out.println("解析CDQ文件成功，共解析 " + result.size() + " 条数据");

                // 打印所有数据
                for (int i = 0; i < result.size(); i++) {
                    PowerForecastData data = result.get(i);
                    System.out.println("数据 " + (i+1) + ": " +
                        "collectTime=" + data.getCollectTime() +
                        ", forecastTime=" + data.getForecastTime() +
                        ", stationCode=" + data.getStationCode() +
                        ", indexCode=" + data.getIndexCode() +
                        ", energyType=" + data.getEnergyType() +
                        ", forecastValue=" + data.getForecastValue() +
                        ", orderNo=" + data.getOrderNo());
                }
            } else {
                System.out.println("解析CDQ文件失败");
            }

            inputStream.close();
        } catch (Exception e) {
            System.err.println("测试CDQ文件解析时出错: " + e.getMessage());
            e.printStackTrace();
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
