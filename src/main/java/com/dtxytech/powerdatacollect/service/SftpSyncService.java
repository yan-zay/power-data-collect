package com.dtxytech.powerdatacollect.service;

/**
 * @Author zay
 * @Date 2025/12/13 15:32
 */

import com.dtxytech.powerdatacollect.config.SftpProperties;
import com.dtxytech.powerdatacollect.config.SyncConfig;
import com.dtxytech.powerdatacollect.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.enums.StationEnum;
import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@Service
//@AllArgsConstructor
public class SftpSyncService {

    private static final List<String> SUFFIX = Arrays.asList("XML", "xml", "WPD", "wpd", "DT", "dt", "RB", "rb", "DAT", "dat", "TXT", "txt", "PPD", "ppd", "PVD", "pvd");

    private Session session;
    private ChannelSftp sftp;

    @Resource
    private SftpProperties sftpConfig;
    @Resource
    private SyncConfig syncConfig;
    private TimeMarkerService timeMarkerService;

    public void syncFileList(IndicatorTypeEnum fileType, Predicate<String> filenameMatcher) {
//        ChannelSftp sftp = connectSftp(fileType);
        for (StationEnum station : StationEnum.values()) {
            log.info("Starting SFTP sync for station: {}", station);
            try {
                checkConnect(); // 确保连接有效
                processStation(fileType, station);
            } catch (Exception e) {
                System.err.println("处理场站失败: " + station + ", 错误: " + e.getMessage());
                // 可记录失败场站，后续重试
            }
        }
    }

    private void processStation(IndicatorTypeEnum fileType, StationEnum station) throws SftpException {
/*        List<RemoteFile> files = new ArrayList<>();
        collectFilesRecursively(sftp, sftpConfig.getRemoteDir(), files, filenameMatcher);

        String lastTime = timeMarkerService.getLastTime(fileType);
        String maxTime = lastTime;

        for (RemoteFile file : files) {
            String timeStr = extractTimeFromFilename(file.filename, fileType);
            if (timeStr != null && timeStr.compareTo(lastTime) > 0) {
                downloadFile(sftp, file.fullPath, sftpConfig.getLocalDownloadDir(), file.filename);
                if (timeStr.compareTo(maxTime) > 0) {
                    maxTime = timeStr;
                }
            }
        }

        if (!maxTime.equals(lastTime)) {
            timeMarkerService.updateLastTime(fileType, maxTime);
        }*/
        String path = "/base/" + station;
        Vector<LsEntry> entries = sftp.ls(path);
        // ... 处理文件
    }

    private void collectFilesRecursively(ChannelSftp sftp, String path, List<RemoteFile> result, Predicate<String> matcher)
            throws Exception {
        @SuppressWarnings("unchecked")
        Vector<LsEntry> entries = sftp.ls(path); // sftp.ls() returns Vector<?>
        if (entries == null) {
            return;
        }

        for (LsEntry entry : entries) {
            String name = entry.getFilename();
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }

            String fullPath = path + "/" + name;
            SftpATTRS attrs = entry.getAttrs();

            if (attrs.isDir()) {
                collectFilesRecursively(sftp, fullPath, result, matcher);
            } else if (matcher.test(name)) {
                result.add(new RemoteFile(fullPath, name));
            }
        }
    }

    private String extractTimeFromFilename(String filename, IndicatorTypeEnum type) {
        if (IndicatorTypeEnum.DQ.equals(type)) {
            // 格式: DTCG__20251214_DQ.WPD → 提取 20251214 (8位)
            if (filename.endsWith("_DQ.WPD")) {
                int underscoreIndex = filename.lastIndexOf('_');
                if (underscoreIndex > 10) {
                    String timePart = filename.substring(underscoreIndex - 10, underscoreIndex);
                    if (timePart.matches("\\d{10}")) {
                        return timePart;
                    }
                }
            }
        } else if (IndicatorTypeEnum.CDQ.equals(type)) {
            // 格式: XMFDC_202512131630_CDQ.WPD → 提取 202512131630 (12位)
            if (filename.endsWith("_CDQ.WPD")) {
                int underscoreIndex = filename.lastIndexOf('_');
                if (underscoreIndex > 12) {
                    String timePart = filename.substring(underscoreIndex - 12, underscoreIndex);
                    if (timePart.matches("\\d{12}")) {
                        return timePart;
                    }
                }
            }
        }
        return null;
    }

    private void downloadFile(ChannelSftp sftp, String remotePath, String localDir, String filename) throws Exception {
        new File(localDir).mkdirs();
        String localPath = localDir + File.separator + filename;
        sftp.get(remotePath, localPath);
        log.info("Downloaded: {} -> {}", remotePath, localPath);
    }

    private static class RemoteFile {
        final String fullPath;
        final String filename;

        RemoteFile(String fullPath, String filename) {
            this.fullPath = fullPath;
            this.filename = filename;
        }
    }

    private void checkConnect() throws JSchException {
        if (session == null || !session.isConnected()) {
            reconnect();
        }
        if (sftp == null || !sftp.isConnected()) {
            openSftpChannel();
        }
    }

    private void reconnect() throws JSchException {
        if (session != null) session.disconnect();
        JSch jsch = new JSch();
        session = jsch.getSession(sftpConfig.getUsername(), sftpConfig.getHost(), sftpConfig.getPort());
        session.setPassword(sftpConfig.getPassword());
        Properties config = new Properties();
        config.put("StrictHostNetKeyChecking", "no");
        config.put("ServerAliveInterval", "60");   // 心跳
        config.put("ServerAliveCountMax", "3");
        session.setConfig(config);
        session.connect(syncConfig.getConnectTimeout()); // 10秒超时
    }

    private void openSftpChannel() throws JSchException {
        if (sftp != null) sftp.disconnect();
        sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect(syncConfig.getConnectTimeout());
    }
}