package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.task.SyncFetchFileTask;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
@ConditionalOnProperty(name = "sftp.region", havingValue = "liaoning", matchIfMissing = false)
public class SftpDownloaderLiaoning extends SftpDownloader {

    @Override
    public List<String> getAllFilePath(IndicatorTypeEnum indicatorType, ChannelSftp sftp, String remoteDir) {
        List<String> filePaths = new ArrayList<>();
        try {
            recurseCollectFilePaths(sftp, remoteDir, indicatorType, filePaths);
        } catch (SftpException | IOException e) {
            throw new RuntimeException("Failed to collect file paths from: " + remoteDir, e);
        }
        return filePaths;
    }

    /**
     * 递归收集远程目录下的所有文件路径
     */
    protected void recurseCollectFilePaths(ChannelSftp sftp, String path, IndicatorTypeEnum indicatorType, List<String> filePaths) throws SftpException, IOException {
        Vector<ChannelSftp.LsEntry> entries = sftp.ls(path);
        if (entries == null) return;
        entries.sort((o1, o2) -> o2.getFilename().compareTo(o1.getFilename()));

        for (ChannelSftp.LsEntry entry : entries) {
            String dirName = entry.getFilename();
            if (isSkippedFolder(dirName)) {
                continue;
            }

            String fullPath = path + SEPARATOR + dirName;
            if (entry.getAttrs().isDir()) {
                // 递归进入子目录
                recurseCollectFilePaths(sftp, fullPath, indicatorType, filePaths);
            } else {
                // 是文件，检查是否符合指标类型并添加到路径列表
//                if (indicatorType.checkFileName(dirName)) {
                if (checkDir(dirName)) {
                    log.info("time: {}", dirName);
                    continue;
                }
                log.info("filePaths: {}", fullPath);
                filePaths.add(fullPath);
//                }
            }
        }
    }

    /**
     * 检查及过滤不需要的文件夹
     * 目录日期是否小于起始日期
     */
    protected boolean checkDir(String dirName) {
        // 获取配置的起始日期
        String fileStartDate = sftpProperties.getFileStartDate();

        int dirDate = extractDateValue(dirName);
        int startCompareValue = extractDateValue(fileStartDate);
        if (dirDate < startCompareValue) {
            return true; // 返回true表示跳过此目录
        }

        // 如果INITIALIZED为true，只处理今天及以后的日期
        if (SyncFetchFileTask.INITIALIZED) {
            int todayValue = extractDateValue(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            // 如果目录日期小于今天，则跳过
            return dirDate < todayValue; // 返回true表示跳过此目录
        }

        return false; // 返回false表示处理此目录
    }

    /**
     * 从字符串中提取日期值用于比较
     * 支持多种日期格式，如yyyyMMdd, yyyy-MM-dd等
     */
    private static int extractDateValue(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        // 移除可能的分隔符，只保留数字
        String[] split = dateStr.split("_");
        String numericStr = "";
        if (split.length == 2) {
            numericStr = split.length == 2 ? split[1] : dateStr.replaceAll("[^0-9]", "");
        } else {
            numericStr = split.length == 3 ? split[1] : dateStr.replaceAll("[^0-9]", "");
        }
        // 尝试转换为整数进行比较，假设格式为yyyyMMdd
        try {
            return Integer.parseInt(numericStr);
        } catch (NumberFormatException e) {
            // 如果无法解析为数字，返回最小值，这样任何有效日期都会大于它
            return Integer.MIN_VALUE;
        }
    }

}
