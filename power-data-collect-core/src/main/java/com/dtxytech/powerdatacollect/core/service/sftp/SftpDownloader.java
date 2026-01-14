package com.dtxytech.powerdatacollect.core.service.sftp;

import com.dtxytech.powerdatacollect.core.config.SftpProperties;
import com.dtxytech.powerdatacollect.core.entity.PowerForecastData;
import com.dtxytech.powerdatacollect.core.enums.IndicatorTypeEnum;
import com.dtxytech.powerdatacollect.core.service.power.PowerForecastDataServiceImpl;
import com.dtxytech.powerdatacollect.core.task.SyncFetchFileTask;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
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
public abstract class SftpDownloader {

    protected static final String SEPARATOR = "/";

    @Autowired
    protected SftpFileParser sftpFileParser;
    @Autowired
    protected PowerForecastDataServiceImpl powerForecastDataService;
    @Autowired
    protected SftpProperties sftpProperties;

    protected abstract List<String> getAllFilePath(IndicatorTypeEnum indicatorType, ChannelSftp sftp, String remoteDir);
}
