//package com.dtxytech.powerdatacollect.service;
//
//import com.dtxytech.powerdatacollect.config.SftpProperties;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.jcraft.jsch.ChannelSftp;
//import com.jcraft.jsch.JSch;
//import com.jcraft.jsch.Session;
//import lombok.AllArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.springframework.stereotype.Service;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Vector;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * @Author zay
// * @Date 2025/12/15 11:07
// */
//@Service
//@Slf4j
//@AllArgsConstructor
//public class SftpFilePullService {
//
//
//    private final SftpProperties sftpProperties;
//    private final SftpSyncStateService stateService;
//
///*    private final String remoteDir;
//    private final String localDownloadDir;
//    private final String host;
//    private final int port;
//    private final String username;
//    private final String password;*/
//
///*    public SftpFilePullService(
//            @Value("${sftp.host}") String host,
//            @Value("${sftp.port}") int port,
//            @Value("${sftp.username}") String username,
//            @Value("${sftp.password}") String password,
//            @Value("${sftp.remote-dir}") String remoteDir,
//            @Value("${app.local-download-dir}") String localDownloadDir,
//            SftpSyncStateService stateService) {
//        this.host = host;
//        this.port = port;
//        this.username = username;
//        this.password = password;
//        this.remoteDir = remoteDir;
//        this.localDownloadDir = localDownloadDir;
//        this.stateService = stateService;
//
//        new File(localDownloadDir).mkdirs();
//    }*/
//
//    // 文件名正则：DTCG_14位数字_(DQ|CDQ).任意扩展名
//    private static final Pattern FILE_PATTERN = Pattern.compile(
//            "^DTCG_(\\d{14})_(DQ|CDQ)\\..+$"
//    );
//
//    public void pullIncrementalFiles() {
//        log.info("开始增量拉取 SFTP 文件...");
//
//        try {
//            JSch jsch = new JSch();
//            Session session = jsch.getSession(username, host, port);
//            session.setPassword(password);
//            session.setConfig("StrictHostKeyChecking", "no");
//            session.connect(10_000);
//
//            ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
//            sftp.connect();
//
//            try {
//                Vector<?> files = sftp.ls(remoteDir);
//                if (files == null) {
//                    log.warn("远程目录为空或无法列出: {}", remoteDir);
//                    return;
//                }
//
//                for (Object obj : files) {
//                    if (!(obj instanceof ChannelSftp.LsEntry)) continue;
//                    ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) obj;
//                    String fileName = entry.getFilename();
//
//                    // 跳过 . 和 ..
//                    if (".".equals(fileName) || "..".equals(fileName)) continue;
//
//                    Matcher matcher = FILE_PATTERN.matcher(fileName);
//                    if (!matcher.matches()) {
//                        log.debug("跳过不符合命名规则的文件: {}", fileName);
//                        continue;
//                    }
//
//                    String timeStr = matcher.group(1); // yyyymmddhhmmss
//                    String indicator = matcher.group(2); // DQ or CDQ
//
//                    if (!stateService.shouldDownload(indicator, timeStr)) {
//                        log.debug("文件已处理或更旧，跳过: {}", fileName);
//                        continue;
//                    }
//
//                    // 下载文件
//                    String remotePath = remoteDir + "/" + fileName;
//                    String localPath = localDownloadDir + "/" + fileName;
//
//                    try (FileOutputStream fos = new FileOutputStream(localPath)) {
//                        sftp.get(remotePath, fos);
//                        log.info("✅ 成功下载: {}", fileName);
//                    }
//
//                    // 更新状态
//                    stateService.updateLatestTime(indicator, timeStr);
//
//                }
//            } finally {
//                sftp.disconnect();
//                session.disconnect();
//            }
//
//        } catch (Exception e) {
//            log.error("SFTP 增量拉取失败", e);
//            throw new RuntimeException("SFTP 拉取异常", e);
//        }
//    }
//}