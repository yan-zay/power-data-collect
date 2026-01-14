package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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
@ConditionalOnProperty(name = "sftp.region", havingValue = "guangxi", matchIfMissing = false)
public class SftpDownloaderGuangxi extends SftpDownloader {

    // 需要跳过的文件夹名称
    private static final String[] SKIPPED_FOLDERS = {"..", "."};

    @Override
    public List<String> getAllFilePath(IndicatorTypeEnum indicatorType, ChannelSftp sftp, String remoteDir) {
        List<String> filePaths = new ArrayList<>();
        try {
            // 列出远程目录下的所有条目
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(remoteDir);
            log.info("SftpDataSyncService syncFileList remoteDir:{}, entries:{}", remoteDir, entries.toString());
            
            if (entries != null) {
                for (ChannelSftp.LsEntry entry : entries) {
                    String fileName = entry.getFilename();
                    log.info("SftpDataSyncService tag 0001, fileName:{}", fileName);
                    
                    // 跳过当前目录和父目录
                    if (isSkippedFolder(fileName)) {
                        log.info("SftpDataSyncService tag 0002.0");
                        continue;
                    }
                    log.info("SftpDataSyncService tag 0002");

                    String dir = remoteDir + SEPARATOR + fileName;
                    getTwoLvDir(indicatorType, sftp, dir, filePaths);
                }
            }
        } catch (SftpException e) {
            throw new RuntimeException("处理远程目录失败: " + remoteDir, e);
        }
        return filePaths;
    }

    private void getTwoLvDir(IndicatorTypeEnum indicatorType, ChannelSftp sftp, String dir, List<String> filePaths) throws SftpException {
        Vector<ChannelSftp.LsEntry> entries = sftp.ls(dir);
        for (ChannelSftp.LsEntry entry : entries) {
            String fileName = entry.getFilename();
            if (isSkippedFolder(fileName)) {
                continue;
            }
            log.info("SftpDataSyncService tag 0003");
            // 检查是否是日期格式的目录（yyyyMMdd）
            if (entry.getAttrs().isDir() && isDateFormat(fileName)) {
                log.info("SftpDataSyncService tag 0004");
                String dateDir = dir + "/" + fileName;
                // 在日期目录中查找CDQYC和DQYC文件
                collectFilePathsFromDateDirectory(sftp, dateDir, indicatorType, filePaths);
            }
        }
    }

    /**
     * 从日期目录收集文件路径
     */
    private void collectFilePathsFromDateDirectory(ChannelSftp sftp, String dateDir, IndicatorTypeEnum indicatorType, List<String> filePaths) {
        log.info("SftpDownloaderGuangxi collectFilePathsFromDateDirectory dateDir:{}", dateDir);
        try {
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(dateDir);
            
            if (entries != null) {
                for (ChannelSftp.LsEntry entry : entries) {
                    String fileName = entry.getFilename();
                    
                    // 跳过当前目录和父目录
                    if (isSkippedFolder(fileName)) {
                        continue;
                    }
                    
                    // 检查文件名是否包含CDQYC或DQYC标识，并且符合指标类型
                    if (!entry.getAttrs().isDir() && isPowerForecastFile(fileName) && isMatchingFileType(fileName, indicatorType)) {
                        String filePath = dateDir + "/" + fileName;
                        filePaths.add(filePath);
                    }
                }
            }
        } catch (SftpException e) {
            throw new RuntimeException("处理日期目录失败: " + dateDir, e);
        }
    }
    
    /**
     * 判断是否是需要跳过的文件夹
     */
    private boolean isSkippedFolder(String fileName) {
        for (String skipped : SKIPPED_FOLDERS) {
            if (skipped.equals(fileName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 判断文件夹名是否是日期格式（yyyyMMdd）
     */
    private boolean isDateFormat(String fileName) {
        // 检查是否符合yyyyMMdd格式
        return fileName.matches("\\d{4}\\d{2}\\d{2}");
    }
    
    /**
     * 判断是否是功率预测文件（包含CDQYC或DQYC）
     */
    private boolean isPowerForecastFile(String fileName) {
        String upperFileName = fileName.toUpperCase();
        return upperFileName.contains("CDQYC") || upperFileName.contains("DQYC");
    }
    
    /**
     * 判断文件是否符合指定的指标类型
     */
    private boolean isMatchingFileType(String fileName, IndicatorTypeEnum indicatorType) {
        String upperFileName = fileName.toUpperCase();
        if (indicatorType == IndicatorTypeEnum.DQ) {
            return upperFileName.contains("DQYC") && !upperFileName.contains("CDQYC");
        } else if (indicatorType == IndicatorTypeEnum.CDQ) {
            return upperFileName.contains("CDQYC");
        }
        return false;
    }
}
