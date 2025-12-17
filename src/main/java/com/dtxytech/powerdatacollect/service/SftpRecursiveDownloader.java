package com.dtxytech.powerdatacollect.service;

import com.dtxytech.powerdatacollect.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.enums.IndicatorTypeEnum;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
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

    private final SftpFileParser sftpFileParser;
    private final TestService testService;

    /**
     * 递归下载并解析指定远程目录下的所有文件
     *
     * @param remoteDir 远程目录路径（如 "/data/logs"）
     * @param fileType
     */
    public void downloadAndParseAllFiles(ChannelSftp sftp, String remoteDir, IndicatorTypeEnum fileType) {
        try {
            recurseDownload(sftp, remoteDir, fileType);
        } catch (SftpException | IOException e) {
            throw new RuntimeException("Failed to recursively download files from: " + remoteDir, e);
        }
    }

    /**
     * 递归遍历远程目录
     */
    private void recurseDownload(ChannelSftp sftp, String remotePath, IndicatorTypeEnum fileType) throws SftpException, IOException {
        Vector<LsEntry> entries = sftp.ls(remotePath);
        if (entries == null) return;

        for (LsEntry entry : entries) {
            String filename = entry.getFilename();
            // 跳过 "." 和 ".."
            if (".".equals(filename) || "..".equals(filename) || !filename.contains(fileType.getName())) {
                continue;
            }

            String fullPath = remotePath + "/" + filename;
            if (entry.getAttrs().isDir()) {
                // 递归进入子目录
                recurseDownload(sftp, fullPath, fileType);
            } else {
                // 是文件，下载并解析
                processFile(sftp, fullPath);
            }
        }
    }

    /**
     * 下载单个文件并调用解析逻辑（预留空方法）
     */
    private void processFile(ChannelSftp sftp, String remoteFilePath) throws SftpException, IOException {
        try (InputStream in = sftp.get(remoteFilePath)) {
            // 预留：文件内容已通过 InputStream 获取
            // 此处可调用业务解析逻辑
            PowerForecastData powerForecastData = sftpFileParser.parseForecastFileFromSftp(remoteFilePath, in);
            log.info("sftpFileParser.parseForecastFileFromSftp Parsing file: {}", powerForecastData);
//            testService.insertData();
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
        // 示例：System.out.println("Parsing file: " + remoteFilePath);
    }
}
