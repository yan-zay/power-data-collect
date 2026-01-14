package com.dtxytech.powerdatacollect.core.test;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.service.sftp.SftpFileParserGuangxi;
import com.dtxytech.powerdatacollect.core.service.station.StationService;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * 测试 SftpFileParserGuangxi 解析功能
 */
@Slf4j
public class TestSftpFileParserGuangxi {

    public static void main(String[] args) {
        StationService mockStationService = new TestSftpFileParserLongjiang.MockStationService();

        SftpFileParserGuangxi parser = new SftpFileParserGuangxi(mockStationService);

        // 测试解析DQ文件
        testDQFile(parser);

        // 测试解析CDQ文件
        testCDQFile(parser);
    }

    private static void testDQFile(SftpFileParserGuangxi parser) {
        String dqFilePath = "D:\\WorkSpace\\DTXY\\power-data-collect\\power-data-collect-starter-boot\\src\\main\\resources\\sftp\\guangxi\\FD_GX.FengPDC_DQYC_20251219_083000.dat";
        String filename = "FD_GX.FengPDC_DQYC_20251219_083000.dat";

        try (FileInputStream fis = new FileInputStream(dqFilePath)) {
            List<PowerForecastData> results = parser.parseForecastFileFromSftp(IndicatorTypeEnum.DQ, fis, "sftp/guangxi", filename);

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
        } catch (IOException e) {
            log.error("读取DQ文件失败: {}", e.getMessage(), e);
        }
    }

    private static void testCDQFile(SftpFileParserGuangxi parser) {
        String cdqFilePath = "D:\\WorkSpace\\DTXY\\power-data-collect\\power-data-collect-starter-boot\\src\\main\\resources\\sftp\\guangxi\\FD_GX.FengPDC_CDQYC_20251201_000000.dat";
        String filename = "FD_GX.FengPDC_CDQYC_20251201_000000.dat";

        try (FileInputStream fis = new FileInputStream(cdqFilePath)) {
            List<PowerForecastData> results = parser.parseForecastFileFromSftp(IndicatorTypeEnum.CDQ, fis, "sftp/guangxi", filename);

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
        } catch (IOException e) {
            log.error("读取CDQ文件失败: {}", e.getMessage(), e);
        }
    }
}
