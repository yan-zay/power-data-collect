package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Vector;

/**
 * @Author zay
 * @Date 2026/1/12 10:28
 */
@Slf4j
@Component
@AllArgsConstructor
@ConditionalOnProperty(name = "sftp.region", havingValue = "longjiang", matchIfMissing = false)
public class SftpDownloaderLongjiang extends SftpDownloader {

    // 定义需要跳过的场站文件夹名称
    private static final String[] SKIPPED_STATIONS = {"ycfile", "yuanjingglyc", "tmp", "..", "."};

    @Override
    protected void downloadAndParseAllFile(ChannelSftp sftp, String remoteDir, IndicatorTypeEnum indicatorType) {
        try {
            // 首先列出根目录下的所有子目录（场站文件夹）
            Vector<?> entries = sftp.ls(remoteDir);
            for (Object entry : entries) {
                com.jcraft.jsch.ChannelSftp.LsEntry lsEntry = (com.jcraft.jsch.ChannelSftp.LsEntry) entry;
                String fileName = lsEntry.getFilename();

                // 跳过当前目录和父目录
                if (".".equals(fileName) || "..".equals(fileName)) {
                    continue;
                }

                // 检查是否是目录且不是需要跳过的目录
                if (lsEntry.getAttrs().isDir() && !isSkippedStation(fileName)) {
                    String stationCode = fileName; // 目录名作为场站编码
                    String stationDir = remoteDir + "/" + stationCode;

                    // 进入场站目录查找bak目录
                    processStationDirectory(sftp, stationDir, stationCode, indicatorType);
                }
            }
        } catch (SftpException e) {
            log.error("处理远程目录 {} 时发生错误", remoteDir, e);
        }
    }

    /**
     * 处理场站目录，查找并处理其中的bak目录
     */
    private void processStationDirectory(ChannelSftp sftp, String stationDir, String stationCode, IndicatorTypeEnum indicatorType) {
        try {
            Vector<?> entries = sftp.ls(stationDir);
            for (Object entry : entries) {
                com.jcraft.jsch.ChannelSftp.LsEntry lsEntry = (com.jcraft.jsch.ChannelSftp.LsEntry) entry;
                String fileName = lsEntry.getFilename();

                // 跳过当前目录和父目录
                if (".".equals(fileName) || "..".equals(fileName)) {
                    continue;
                }

                // 查找名为"bak"的目录
                if ("bak".equals(fileName) && lsEntry.getAttrs().isDir()) {
                    String bakDir = stationDir + "/bak";
                    processBakDirectory(sftp, bakDir, stationCode, indicatorType);
                }
            }
        } catch (SftpException e) {
            log.error("处理场站目录 {} 时发生错误", stationDir, e);
        }
    }

    /**
     * 处理bak目录，遍历日期子目录
     */
    private void processBakDirectory(ChannelSftp sftp, String bakDir, String stationCode, IndicatorTypeEnum indicatorType) {
        try {
            Vector<?> entries = sftp.ls(bakDir);
            for (Object entry : entries) {
                com.jcraft.jsch.ChannelSftp.LsEntry lsEntry = (com.jcraft.jsch.ChannelSftp.LsEntry) entry;
                String fileName = lsEntry.getFilename();

                // 跳过当前目录和父目录
                if (".".equals(fileName) || "..".equals(fileName)) {
                    continue;
                }

                // 假设目录名是日期格式（如 2025-10-23），检查是否是目录
                if (lsEntry.getAttrs().isDir()) {
                    String dateDir = bakDir + "/" + fileName;
                    processDateDirectory(sftp, dateDir, stationCode, indicatorType);
                }
            }
        } catch (SftpException e) {
            log.error("处理bak目录 {} 时发生错误", bakDir, e);
        }
    }

    /**
     * 处理日期目录，下载和解析其中的WPD文件
     */
    private void processDateDirectory(ChannelSftp sftp, String dateDir, String stationCode, IndicatorTypeEnum indicatorType) {
        try {
            Vector<?> entries = sftp.ls(dateDir);
            for (Object entry : entries) {
                com.jcraft.jsch.ChannelSftp.LsEntry lsEntry = (com.jcraft.jsch.ChannelSftp.LsEntry) entry;
                String fileName = lsEntry.getFilename();

                // 跳过当前目录和父目录
                if (".".equals(fileName) || "..".equals(fileName)) {
                    continue;
                }

                // 检查是否是WPD文件
                if (fileName.toLowerCase().endsWith(".wpd")) {
                    String filePath = dateDir + "/" + fileName;
                    log.info("正在处理文件: {}, 场站: {}, 类型: {}", filePath, stationCode, indicatorType);

                    // 下载文件并解析
                    sftpFileParser.parseForecastFileFromSftp(indicatorType, sftp.get(filePath), filePath, fileName);
                }
            }
        } catch (SftpException e) {
            log.error("处理日期目录 {} 时发生错误", dateDir, e);
        }
    }

    /**
     * 判断是否是需要跳过的场站目录
     */
    private boolean isSkippedStation(String stationName) {
        for (String skipped : SKIPPED_STATIONS) {
            if (skipped.equalsIgnoreCase(stationName)) {
                return true;
            }
        }
        return false;
    }
}
