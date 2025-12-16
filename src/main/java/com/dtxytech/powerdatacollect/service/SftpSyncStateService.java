package com.dtxytech.powerdatacollect.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author zay
 * @Date 2025/12/15 11:06
 */
@Component
@Slf4j
public class SftpSyncStateService {

    private final ObjectMapper objectMapper = new ObjectMapper();
//    @Value("${app.state-file}")
    private String stateFilePath;

    // 内存中的状态：指标 -> 最新时间字符串
    private volatile Map<String, String> latestTimeMap = new ConcurrentHashMap<>();

    /**
     * 加载状态文件到内存
     */
    @SuppressWarnings("unchecked")
    private void loadStateFromFile() {
        File file = new File(stateFilePath);
        if (!file.exists()) {
            log.info("状态文件不存在，初始化为空: {}", stateFilePath);
            latestTimeMap = new ConcurrentHashMap<>();
            return;
        }

        try {
            Map<String, String> loaded = objectMapper.readValue(file, Map.class);
            this.latestTimeMap = new ConcurrentHashMap<>(loaded);
            log.info("加载状态成功: {}", latestTimeMap);
        } catch (Exception e) {
            log.error("加载状态文件失败，使用空状态", e);
            this.latestTimeMap = new ConcurrentHashMap<>();
        }
    }

    /**
     * 保存当前状态到文件
     */
    public void saveStateToFile() {
        try {
            // 确保目录存在
            new File(stateFilePath).getParentFile().mkdirs();

            objectMapper.writeValue(new File(stateFilePath), latestTimeMap);
            log.debug("状态已保存: {}", latestTimeMap);
        } catch (Exception e) {
            log.error("保存状态文件失败", e);
        }
    }

    /**
     * 判断是否需要下载该文件
     *
     * @param indicator 指标名（DQ / CDQ）
     * @param fileTime  文件时间字符串（yyyyMMddHHmmss）
     * @return true 表示需要下载
     */
    public boolean shouldDownload(String indicator, String fileTime) {
        String lastTime = latestTimeMap.get(indicator);
        if (lastTime == null) {
            return true; // 从未下载过
        }
        return fileTime.compareTo(lastTime) > 0; // 字符串比较即可
    }

    /**
     * 更新某个指标的最新时间（线程安全）
     */
    public void updateLatestTime(String indicator, String newTime) {
        latestTimeMap.put(indicator, newTime);
        saveStateToFile(); // 立即持久化，避免丢失
    }

    public Map<String, String> getLatestTimeMap() {
        return new HashMap<>(latestTimeMap);
    }
}
