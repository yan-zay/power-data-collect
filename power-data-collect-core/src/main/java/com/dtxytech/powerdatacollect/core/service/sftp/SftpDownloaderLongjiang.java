package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.task.SyncFetchFileTask;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
    public List<String> getAllFilePath(IndicatorTypeEnum indicatorType, ChannelSftp sftp, String remoteDir) {
        List<String> filePaths = new ArrayList<>();
        try {
            // 首先列出根目录下的所有子目录（场站文件夹）
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(remoteDir);
            log.info("SftpDataSyncService syncFileList indicatorType:{}, remoteDir:{}, entries:{}", indicatorType, remoteDir, entries);
            for (ChannelSftp.LsEntry entry : entries) {
                String fileName = entry.getFilename();

                // 跳过当前目录和父目录
                if (".".equals(fileName) || "..".equals(fileName)) {
                    continue;
                }
                // 检查是否是目录且不是需要跳过的目录
                if (entry.getAttrs().isDir() && !isSkippedStation(fileName)) {
                    String stationCode = fileName; // 目录名作为场站编码
                    String stationDir = remoteDir + "/" + stationCode;

                    // 进入场站目录查找bak目录
                    collectFilePathsFromStation(sftp, stationDir, stationCode, indicatorType, filePaths);
                }
            }
        } catch (SftpException e) {
            log.error("处理远程目录 {} 时发生错误", remoteDir, e);
        }
        return filePaths;
    }

    /**
     * 从场站目录收集文件路径
     */
    private void collectFilePathsFromStation(ChannelSftp sftp, String stationDir, String stationCode, IndicatorTypeEnum indicatorType, List<String> filePaths) {
        try {
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(stationDir);
            for (ChannelSftp.LsEntry entry : entries) {
                String fileName = entry.getFilename();

                // 跳过当前目录和父目录
                if (".".equals(fileName) || "..".equals(fileName)) {
                    continue;
                }

                // 查找名为"bak"的目录
                if ("bak".equals(fileName) && entry.getAttrs().isDir()) {
                    String bakDir = stationDir + "/bak";
                    collectFilePathsFromBak(sftp, bakDir, stationCode, indicatorType, filePaths);
                }
            }
        } catch (SftpException e) {
            log.error("处理场站目录 {} 时发生错误", stationDir, e);
        }
    }

    /**
     * 从bak目录收集文件路径
     */
    private void collectFilePathsFromBak(ChannelSftp sftp, String bakDir, String stationCode, IndicatorTypeEnum indicatorType, List<String> filePaths) {
        try {
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(bakDir);
            for (ChannelSftp.LsEntry entry : entries) {
                String fileName = entry.getFilename();

                // 跳过当前目录和父目录
                if (".".equals(fileName) || "..".equals(fileName)) {
                    continue;
                }

                // 假设目录名是日期格式（如 2025-10-23），检查是否是目录
                if (entry.getAttrs().isDir() && checkDirDate(fileName)){
                    String dateDir = bakDir + "/" + fileName;
                    collectFilePathsFromDateDir(sftp, dateDir, stationCode, indicatorType, filePaths);
                }
            }
        } catch (SftpException e) {
            log.error("处理bak目录 {} 时发生错误", bakDir, e);
        }
    }

    /**
     * 验证文件名中的日期格式是否为yyyy-MM-dd且日期是否满足要求
     * 如果已初始化，则日期必须大于等于今天；否则，日期必须大于等于配置的起始日期
     */
    private boolean checkDirDate(String fileName) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String configDateStr = sftpProperties.getFileStartDate();
        if (fileName.length() != 10) {
            return false;
        }
        LocalDate fileDate = LocalDate.parse(fileName, formatter);
        LocalDate configDate = LocalDate.parse(configDateStr, formatter);
        // 检查是否大于等于起始日期
        if (fileDate.isEqual(configDate) || fileDate.isAfter(configDate)) {
            return true;
        }
        // 如果已初始化，检查是否大于等于今天
        LocalDate now = LocalDate.now();
        if (SyncFetchFileTask.INITIALIZED && (fileDate.isEqual(now) || fileDate.isAfter(now))) {
            return true;
        }
        return false;
    }


    /**
     * 从日期目录收集文件路径
     */
    private void collectFilePathsFromDateDir(ChannelSftp sftp, String dateDir, String stationCode, IndicatorTypeEnum indicatorType, List<String> filePaths) {
        try {
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(dateDir);
            for (ChannelSftp.LsEntry entry : entries) {
                String fileName = entry.getFilename();

                // 跳过当前目录和父目录
                if (".".equals(fileName) || "..".equals(fileName)) {
                    continue;
                }

                // 检查是否是WPD文件且符合指标类型
                if (fileName.toLowerCase().endsWith(".WPD") && isMatchingFileType(fileName, indicatorType)) {
                    String filePath = dateDir + "/" + fileName;
                    filePaths.add(filePath);
                }
            }
        } catch (SftpException e) {
            log.error("处理日期目录 {} 时发生错误", dateDir, e);
        }
    }

    /**
     * 根据指标类型检查文件名是否匹配
     */
    private boolean isMatchingFileType(String fileName, IndicatorTypeEnum indicatorType) {
        return indicatorType.checkFileName(fileName);
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
