package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.config.SftpProperties;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SFTP连接管理器：为每个线程提供独立的SFTP连接，同时也支持为并行任务创建独立连接
 * @Author zay
 * @Date 2025/12/17 16:00
 */
@Component
@AllArgsConstructor
@Slf4j
public class SftpConnectionManager {

    // 使用 ThreadLocal 确保每个线程拥有独立的连接（主要用于主线程）
    private static final ThreadLocal<ChannelSftp> sftpThreadLocal = new ThreadLocal<>();

    // 用于并行任务的连接缓存（如果需要支持大量并行连接，可以考虑连接池）
    private static final ConcurrentHashMap<Long, ChannelSftp> parallelConnections = new ConcurrentHashMap<>();

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
     * 为并行任务创建新的 SFTP 连接（每个线程需要独立连接时调用）
     */
    public ChannelSftp createNewSftpConnection() {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(sftpConfig.getUsername(), sftpConfig.getHost(), sftpConfig.getPort());
            session.setPassword(sftpConfig.getPassword());

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect(5000);

            ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();

            // 存储到并行连接映射中，以当前线程ID作为key
            long threadId = Thread.currentThread().getId();
            parallelConnections.put(threadId, sftp);
            log.debug("Created new SFTP connection for thread: {}", threadId);
            return sftp;
        } catch (JSchException e) {
            throw new RuntimeException("Failed to create new SFTP connection", e);
        }
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
                log.warn("Error disconnecting SFTP connection", e);
            } finally {
                sftpThreadLocal.remove();
            }
        }

        // 清理并行连接
        long threadId = Thread.currentThread().getId();
        ChannelSftp parallelSftp = parallelConnections.remove(threadId);
        if (parallelSftp != null) {
            try {
                if (parallelSftp.isConnected()) {
                    parallelSftp.disconnect();
                }
                Session session = parallelSftp.getSession();
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            } catch (JSchException e) {
                log.warn("Error disconnecting parallel SFTP connection for thread: {}", threadId, e);
            }
        }
    }

    /**
     * 释放特定线程的并行连接
     */
    public static void releaseParallelConnection(long threadId) {
        ChannelSftp sftp = parallelConnections.remove(threadId);
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
                log.warn("Error releasing parallel SFTP connection for thread: {}", threadId, e);
            }
        }
    }
}
