package com.dtxytech.powerdatacollect.service.sftp;

import com.dtxytech.powerdatacollect.config.SftpProperties;
import com.jcraft.jsch.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * A类：为当前线程提供独占的 SFTP 连接。
 * 每个线程拥有自己的 Session 和 ChannelSftp，避免并发问题。
 * @Author zay
 * @Date 2025/12/17 16:00
 */
@Component
@AllArgsConstructor
public class SftpConnectionManager {

    // 使用 ThreadLocal 确保每个线程拥有独立的连接
    private static final ThreadLocal<ChannelSftp> sftpThreadLocal = new ThreadLocal<>();

    private SftpProperties sftpConfig;

    /**
     * 创建并绑定 SFTP 连接到当前线程
     */
    public void reConnect() {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(sftpConfig.getUsername(), sftpConfig.getHost(), sftpConfig.getPort());
            session.setPassword(sftpConfig.getPassword());

            // 安全配置：生产环境应设为 "yes" 并加载 known_hosts
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no"); // 测试用，生产建议 "yes"
            session.setConfig(config);

            session.connect(5000); // 5秒超时

            ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();

            sftpThreadLocal.set(sftp);
        } catch (JSchException e) {
            throw new RuntimeException("Failed to establish SFTP connection", e);
        }
    }

    /**
     * 获取当前线程的 SFTP 连接
     */
    public ChannelSftp getCurrentSftp() {
        ChannelSftp sftp = sftpThreadLocal.get();
        if (sftp == null || !sftp.isConnected()) {
            reConnect();
            return sftpThreadLocal.get();
        }
        return sftp;
    }

    /**
     * 关闭并移除当前线程的 SFTP 连接
     */
    public static void disConnection() {
        ChannelSftp sftp = sftpThreadLocal.get();
        if (sftp != null) {
            try {
                if (sftp.isConnected()) {
                    sftp.disconnect();
                }
                
                Session session = sftp.getSession();
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            } catch (JSchException e) {
                // 记录异常但不中断资源清理过程
                // 可以使用日志记录器记录详细信息
            } finally {
                sftpThreadLocal.remove();
            }
        }
    }
}
