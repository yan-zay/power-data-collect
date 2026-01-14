package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.task.SyncFetchFileTask;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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


    private static final String SEPARATOR = "/";

    /**
     * 递归下载并解析指定远程目录下的所有文件
     */
    @Override
    protected void downloadAndParseAllFile(ChannelSftp sftp, String remoteDir, IndicatorTypeEnum indicatorType) {
        try {
            recurseDownload(sftp, remoteDir, indicatorType);
        } catch (SftpException | IOException e) {
            throw new RuntimeException("Failed to recursively download files from: " + remoteDir, e);
        }
    }


    /**
     * 递归遍历远程目录
     */
    protected void recurseDownload(ChannelSftp sftp, String path, IndicatorTypeEnum indicatorType) throws SftpException, IOException {
        Vector<ChannelSftp.LsEntry> entries = sftp.ls(path);
        if (entries == null) return;
        entries.sort((o1, o2) -> o2.getFilename().compareTo(o1.getFilename()));

        for (ChannelSftp.LsEntry entry : entries) {
            String dirName = entry.getFilename();
            // 跳过 "." 和 ".."
            if (".".equals(dirName) || "..".equals(dirName)) {
                continue;
            }

            String fullPath = path + SEPARATOR + dirName;
            log.info("读取文件路径{}",fullPath);
            if (entry.getAttrs().isDir()) {
                // 递归进入子目录
                recurseDownload(sftp, fullPath, indicatorType);
            } else {
                // 是文件，下载并解析
                processFile(sftp, path, dirName, indicatorType);
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
        String numericStr="";
        numericStr = split.length==3?split[1]:dateStr.replaceAll("[^0-9]", "");
        // 尝试转换为整数进行比较，假设格式为yyyyMMdd
        try {
            return Integer.parseInt(numericStr);
        } catch (NumberFormatException e) {
            // 如果无法解析为数字，返回最小值，这样任何有效日期都会大于它
            return Integer.MIN_VALUE;
        }
    }

    /**
     * 下载单个文件并调用解析逻辑
     */
    private void processFile(ChannelSftp sftp, String path, String filename, IndicatorTypeEnum indicatorType) throws SftpException, IOException {
        String fullPath = path + SEPARATOR + filename;
        if (!indicatorType.checkFileName(filename)) {
            return;
        }
        if (checkDir(filename)) {
            return;
        }
        try (InputStream in = sftp.get(fullPath)) {
            // 预留：文件内容已通过 InputStream 获取
            // 此处可调用业务解析逻辑
            List<PowerForecastData> list = sftpFileParser.parseForecastFileFromSftp(indicatorType, in, path, filename);
            log.info("sftpFileParser.parseForecastFileFromSftp, Parsing filename: {}, list.size:{}", filename, list.size());
            powerForecastDataService.saveList(list);
        }
    }
}
