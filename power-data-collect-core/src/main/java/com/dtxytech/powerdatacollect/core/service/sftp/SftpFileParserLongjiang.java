package com.dtxytech.powerdatacollect.core.service.sftp;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @Author zay
 * @Date 2025/12/16 17:26
 */
@Component
@AllArgsConstructor
@ConditionalOnProperty(name = "sftp.region", havingValue = "longjiang", matchIfMissing = false)
public class SftpFileParserLongjiang extends SftpFileParser {


}
