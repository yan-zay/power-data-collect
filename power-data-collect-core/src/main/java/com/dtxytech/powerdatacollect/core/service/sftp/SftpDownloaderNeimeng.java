package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
@ConditionalOnProperty(name = "sftp.region", havingValue = "neimeng", matchIfMissing = false)
public class SftpDownloaderNeimeng extends SftpDownloader {

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
    protected void recurseCollectFilePaths(ChannelSftp sftp, String path, IndicatorTypeEnum indicatorType,
                                           List<String> filePaths) throws SftpException, IOException {
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
                if (getPathLv(fullPath) > 4 && checkFileDate(dirName)) {
                    continue;
                }

                // 递归进入子目录
                recurseCollectFilePaths(sftp, fullPath, indicatorType, filePaths);
            } else {
                // 是文件，检查是否符合指标类型并添加到路径列表
                if (indicatorType.checkFileName(dirName) && checkFullPathDepth(fullPath)) {
                    filePaths.add(fullPath);
                }
            }
        }
    }

    private boolean checkFullPathDepth(String fullPath) {
        int pathLv = getPathLv(fullPath);
        return pathLv >= 5 ;
    }

    private static int getPathLv(String fullPath) {
        // 统一处理路径分隔符，将Windows和Unix风格的分隔符都转换为"/"
        String normalizedPath = fullPath.replace("\\", "/");
        // 分割路径
        String[] parts = normalizedPath.split("/");
        return parts.length- 1;
    }
}
