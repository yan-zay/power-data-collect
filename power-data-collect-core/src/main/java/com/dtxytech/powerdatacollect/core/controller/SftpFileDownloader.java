package com.dtxytech.powerdatacollect.core.controller;

/**
 * @Author zay
 * @Date 2025/12/13 15:47
 */

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SftpFileDownloader {

    /**
     * 连接远程 SFTP 服务器并下载指定文件到本地
     *
     * @param host         远程服务器地址（如 "192.168.1.100"）
     * @param port         端口（通常为 22）
     * @param username     用户名
     * @param password     密码
     * @param remotePath   远程文件完整路径（如 "/data/DTDL4_2025121315_DQ.WPD"）
     * @param localPath    本地保存路径（如 "C:/downloads/file.WPD" 或 "./file.WPD"）
     * @return             true 表示成功，false 表示失败
     */
    public static boolean downloadFileFromSftp(
            String host,
            int port,
            String username,
            String password,
            String remotePath,
            String localPath) {

        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            // 1. 建立 SSH 会话
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(5000); // 5秒超时

            // 2. 打开 SFTP 通道
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect(5000);

            // 3. 创建本地目录（如果不存在）
            File localFile = new File(localPath);
            localFile.getParentFile().mkdirs();

            // 4. 下载文件
//            sftpChannel.get
            try (InputStream inputStream = sftpChannel.get(remotePath);
                 FileOutputStream outputStream = new FileOutputStream(localFile)) {

                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
            }

            System.out.println("✅ 文件下载成功: " + remotePath + " -> " + localPath);
            return true;

        } catch (Exception e) {
            System.err.println("❌ 下载失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // 5. 清理资源
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    // ===== 示例用法 =====
    public static void main(String[] args) {
        boolean success = downloadFileFromSftp(
                "172.30.1.25",   // host
                22,                       // port
                "sftp",                   // username
                "JCDZ@sp.0",               // password
                "/home/ies/success/CF_Cg/20251213/DTCG__20251214_DQ.WPD", // remotePath
                "./downloads/DTCG__20251214_DQ.WPD"   // localPath
        );

        if (success) {
            System.out.println("🎉 操作完成！");
        } else {
            System.out.println("💥 操作失败，请检查网络、账号或路径。");
        }
    }
}