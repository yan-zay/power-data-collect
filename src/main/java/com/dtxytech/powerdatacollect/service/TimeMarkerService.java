package com.dtxytech.powerdatacollect.service;

import com.dtxytech.powerdatacollect.enums.IndicatorTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @Author zay
 * @Date 2025/12/13 15:12
 */
@Slf4j
@Service
public class TimeMarkerService {

    private static final String MARKER_FILE = "last_fetch_time.properties";
    private final Path markerPath = Paths.get(MARKER_FILE);
    private final Map<IndicatorTypeEnum, String> markers = new HashMap<>();

    @PostConstruct
    public void load() {
        if (!Files.exists(markerPath)) {
            log.info("Marker file not found, initializing defaults.");
            markers.put(IndicatorTypeEnum.DQ, "1970010100");
            markers.put(IndicatorTypeEnum.CDQ, "197001010000");
            save();
            return;
        }

        try {
            Properties props = new Properties();
            props.load(Files.newInputStream(markerPath));
            markers.put(IndicatorTypeEnum.DQ, props.getProperty(IndicatorTypeEnum.DQ.getName(), "1970010100"));
            markers.put(IndicatorTypeEnum.CDQ, props.getProperty(IndicatorTypeEnum.CDQ.getName(), "197001010000"));
            log.info("Loaded time markers: {}", markers);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load marker file", e);
        }
    }

    public String getLastTime(IndicatorTypeEnum type) {
        return markers.getOrDefault(type, type.equals(IndicatorTypeEnum.DQ) ? "1970010100" : "197001010000");
    }

    public synchronized void updateLastTime(IndicatorTypeEnum type, String newTime) {
        String current = markers.get(type);
        if (newTime != null && newTime.compareTo(current) > 0) {
            markers.put(type, newTime);
            save();
        }
    }

    private void save() {
        try {
            Properties props = new Properties();
            props.setProperty(IndicatorTypeEnum.DQ.getName(), markers.get(IndicatorTypeEnum.DQ));
            props.setProperty(IndicatorTypeEnum.CDQ.getName(), markers.get(IndicatorTypeEnum.CDQ));
            props.store(Files.newOutputStream(markerPath), "Last fetched timestamps for SFTP sync");
        } catch (IOException e) {
            log.error("Failed to save marker file", e);
        }
    }
}
