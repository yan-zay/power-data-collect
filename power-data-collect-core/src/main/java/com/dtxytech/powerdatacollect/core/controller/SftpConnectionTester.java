package com.dtxytech.powerdatacollect.core.controller;

/**
 * @Author zay
 * @Date 2025/12/13 15:47
 */
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SftpConnectionTester {

    /**
     * 测试是否能连接到远程 SFTP 服务器
     *
     * @param host     服务器地址
     * @param port     端口（通常 22）
     * @param username 用户名
     * @param password 密码
     * @return true 表示连接成功，false 表示失败
     */
    public static boolean testSftpConnection(String host, int port, String username, String password) {
        Session session = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no"); // 跳过主机密钥检查（测试用）
            session.connect(5000); // 超时 5 秒

            log.info("✅ 成功连接到 SFTP 服务器: {}:{}", host, port);
            return true;
        } catch (Exception e) {
            log.warn("❌ 无法连接到 SFTP 服务器 {}:{} - {}", host, port, e.getMessage());
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    // 示例用法
    public static void main(String[] args) {
        boolean connected = testSftpConnection("172.30.1.25", 22, "sftp", "JCDZ@sp.0");
        if (connected) {
            System.out.println("连接成功！");
        } else {
            System.out.println("连接失败！");
        }
    }
}
