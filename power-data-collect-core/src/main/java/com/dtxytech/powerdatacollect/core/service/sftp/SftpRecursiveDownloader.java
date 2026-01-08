package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.config.SftpProperties;
import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.service.power.PowerForecastDataServiceImpl;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Vector;

/**
 * B类：使用给定的 SFTP 连接，递归拉取指定远程目录下所有文件，
 * 并对每个文件调用预留的解析方法。
 *
 * @Author zay
 * @Date 2025/12/17 16:01
 */
@Slf4j
@Component
@AllArgsConstructor
public class SftpRecursiveDownloader {

    private static final String SEPARATOR = "/";
    public volatile static boolean INITIALIZED = false;

    private final SftpFileParser sftpFileParser;
    private final PowerForecastDataServiceImpl powerForecastDataService;
    private final SftpProperties sftpProperties;

    /**
     * 递归下载并解析指定远程目录下的所有文件
     *
     * @param remoteDir 远程目录路径（如 "/data/logs"）
     * @param indicatorType 指标类型
     */
    public void downloadAndParseAllFile(ChannelSftp sftp, String remoteDir, IndicatorTypeEnum indicatorType) {
        try {
            recurseDownload(sftp, remoteDir, indicatorType);
        } catch (SftpException | IOException e) {
            throw new RuntimeException("Failed to recursively download files from: " + remoteDir, e);
        }
    }

    /**
     * 递归遍历远程目录
     */
    private void recurseDownload(ChannelSftp sftp, String path, IndicatorTypeEnum indicatorType) throws SftpException, IOException {
        Vector<LsEntry> entries = sftp.ls(path);
        if (entries == null) return;
        entries.sort((o1, o2) -> o2.getFilename().compareTo(o1.getFilename()));

        for (LsEntry entry : entries) {
            String filename = entry.getFilename();
            // 跳过 "." 和 ".."
            if (".".equals(filename) || "..".equals(filename)) {
                continue;
            }

            String fullPath = path + SEPARATOR + filename;
            if (entry.getAttrs().isDir()) {
                if (checkDirDate(fullPath)) {
                    continue;
                }
                // 递归进入子目录
                recurseDownload(sftp, fullPath, indicatorType);
            } else {
                // 是文件，下载并解析
                processFile(sftp, path, filename, indicatorType);
            }
        }
    }

    private static boolean checkDirDate(String fullPath) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                .compareTo(SftpFileParser.getPathPart(fullPath, 4)) < 0 && INITIALIZED;
    }

    /**
     * 下载单个文件并调用解析逻辑（预留空方法）
     */
    private void processFile(ChannelSftp sftp, String path, String filename, IndicatorTypeEnum indicatorType) throws SftpException, IOException {
        String fullPath = path + SEPARATOR + filename;
        if (!indicatorType.checkFileName(filename)) {
            return;
        }
        try (InputStream in = sftp.get(fullPath)) {
            // 预留：文件内容已通过 InputStream 获取
            // 此处可调用业务解析逻辑
            List<PowerForecastData> list = sftpFileParser.parseForecastFileFromSftp(indicatorType, in, path, filename);
            log.info("sftpFileParser.parseForecastFileFromSftp, Parsing file: {}, data size:{}", filename, list.size());
            if (list.isEmpty()) {
                return;
            }
            boolean exist = powerForecastDataService.checkDuplicate(list.get(0));
            if (exist) {
                log.info("powerForecastDataService.checkDuplicate exist, path:{}, filename:{}", path, filename);
                return;
            }
            boolean saved = powerForecastDataService.saveBatch(list);
            if (saved) {
                log.error("powerForecastDataService.saveBatch error, list:{}", list);
            }
        }
    }

    /**
     * 【预留】解析文件内容的方法（由业务方实现）
     *
     * @param remoteFilePath 远程文件路径（用于日志或元数据）
     * @param contentStream  文件内容输入流
     */
    protected void parseFileContent(String remoteFilePath, InputStream contentStream) {
        // TODO: 实现具体的文件解析逻辑（如 JSON 解析、日志分析等）
    }
}
