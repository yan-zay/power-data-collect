package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Vector;

/**
 * @Author zay
 * @Date 2026/1/12 10:28
 */
@Component
@AllArgsConstructor
@ConditionalOnProperty(name = "sftp.region", havingValue = "guangxi", matchIfMissing = false)
public class SftpDownloaderGuangxi extends SftpDownloader {

    // 需要跳过的文件夹名称
    private static final String[] SKIPPED_FOLDERS = {"..", "."};

    @Override
    protected void downloadAndParseAllFile(ChannelSftp sftp, String remoteDir, IndicatorTypeEnum indicatorType) {
        try {
            // 列出远程目录下的所有条目
            Vector<?> entries = sftp.ls(remoteDir);
            
            if (entries != null) {
                for (Object entryObj : entries) {
                    com.jcraft.jsch.ChannelSftp.LsEntry entry = (com.jcraft.jsch.ChannelSftp.LsEntry) entryObj;
                    String fileName = entry.getFilename();
                    
                    // 跳过当前目录和父目录
                    if (isSkippedFolder(fileName)) {
                        continue;
                    }
                    
                    // 检查是否是日期格式的目录（yyyy-MM-dd）
                    if (entry.getAttrs().isDir() && isDateFormat(fileName)) {
                        String dateDir = remoteDir + "/" + fileName;
                        
                        // 在日期目录中查找CDQYC和DQYC文件
                        processDateDirectory(sftp, dateDir, indicatorType);
                    }
                }
            }
        } catch (SftpException e) {
            throw new RuntimeException("处理远程目录失败: " + remoteDir, e);
        }
    }
    
    /**
     * 处理日期目录，查找并处理其中的CDQYC和DQYC文件
     */
    private void processDateDirectory(ChannelSftp sftp, String dateDir, IndicatorTypeEnum indicatorType) {
        try {
            Vector<?> entries = sftp.ls(dateDir);
            
            if (entries != null) {
                for (Object entryObj : entries) {
                    com.jcraft.jsch.ChannelSftp.LsEntry entry = (com.jcraft.jsch.ChannelSftp.LsEntry) entryObj;
                    String fileName = entry.getFilename();
                    
                    // 跳过当前目录和父目录
                    if (isSkippedFolder(fileName)) {
                        continue;
                    }
                    
                    // 检查文件名是否包含CDQYC或DQYC标识
                    if (!entry.getAttrs().isDir() && isPowerForecastFile(fileName)) {
                        String filePath = dateDir + "/" + fileName;
                        
                        // 根据文件名判断指标类型并解析文件
                        IndicatorTypeEnum fileType = determineIndicatorType(fileName);
                        if (fileType != null && fileType == indicatorType) {
                            // 下载并解析文件
                            sftpFileParser.parseForecastFileFromSftp(indicatorType, sftp.get(filePath), dateDir, fileName);
                        }
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
     * 判断文件夹名是否是日期格式（yyyy-MM-dd）
     */
    private boolean isDateFormat(String fileName) {
        // 检查是否符合yyyy-MM-dd格式
        return fileName.matches("\\d{4}-\\d{2}-\\d{2}");
    }
    
    /**
     * 判断是否是功率预测文件（包含CDQYC或DQYC）
     */
    private boolean isPowerForecastFile(String fileName) {
        String upperFileName = fileName.toUpperCase();
        return upperFileName.contains("CDQYC") || upperFileName.contains("DQYC");
    }
    
    /**
     * 根据文件名确定指标类型
     */
    private IndicatorTypeEnum determineIndicatorType(String fileName) {
        String upperFileName = fileName.toUpperCase();
        
        if (upperFileName.contains("CDQYC")) {
            return IndicatorTypeEnum.CDQ;
        } else if (upperFileName.contains("DQYC")) {
            return IndicatorTypeEnum.DQ;
        }
        
        return null;
    }
}
