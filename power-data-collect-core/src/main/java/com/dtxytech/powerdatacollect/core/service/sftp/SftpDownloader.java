package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.config.SftpProperties;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.service.power.PowerForecastDataServiceImpl;
import com.dtxytech.powerdatacollect.core.task.SyncFetchFileTask;
import com.jcraft.jsch.ChannelSftp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * B类：使用给定的 SFTP 连接，递归拉取指定远程目录下所有文件，
 * 并对每个文件调用预留的解析方法。
 *
 * @Author zay
 * @Date 2025/12/17 16:01
 */
@Slf4j
public abstract class SftpDownloader {

    protected static final String SEPARATOR = "/";
    // 需要跳过的文件夹名称
    protected static final String[] SKIPPED_FOLDERS = {"..", "."};

    @Autowired
    protected SftpFileParser sftpFileParser;
    @Autowired
    protected PowerForecastDataServiceImpl powerForecastDataService;
    @Autowired
    protected SftpProperties sftpProperties;

    protected abstract List<String> getAllFilePath(IndicatorTypeEnum indicatorType, ChannelSftp sftp, String remoteDir);

    /**
     * 判断是否是需要跳过的文件夹
     */
    protected boolean isSkippedFolder(String fileName) {
        for (String skipped : SKIPPED_FOLDERS) {
            if (skipped.equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查及过滤不需要的文件夹
     * 目录日期是否小于起始日期
     * dirName: format yyyyMMdd
     */
    protected boolean checkFileDate(String dirName) {
        // 首先检查目录名是否为纯数字
        if (!isNumeric(dirName)) {
            return true; // 不是纯数字，跳过此目录
        }
        // 获取配置的起始日期
        String fileStartDate = sftpProperties.getFileStartDate();

        int dirDate = extractDateValue(dirName);
        int startCompareValue = extractDateValue(fileStartDate);
        if (dirDate < startCompareValue) {
            return true; // 返回true表示跳过此目录
        }

        // 如果INITIALIZED为true，只处理今天及以后的日期
        if (SyncFetchFileTask.INITIALIZED) {
            int todayValue = extractDateValue(LocalDate.now().minusDays(3).format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            // 如果目录日期小于今天，则跳过
            return dirDate < todayValue; // 返回true表示跳过此目录
        }

        return false; // 返回false表示处理此目录
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
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
        String numericStr = dateStr.replaceAll("[^0-9]", "");
        // 尝试转换为整数进行比较，假设格式为yyyyMMdd
        try {
            return Integer.parseInt(numericStr);
        } catch (NumberFormatException e) {
            // 如果无法解析为数字，返回最小值，这样任何有效日期都会大于它
            return Integer.MIN_VALUE;
        }
    }
}
