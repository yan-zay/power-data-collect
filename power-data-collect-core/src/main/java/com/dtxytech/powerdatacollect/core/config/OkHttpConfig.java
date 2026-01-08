package com.dtxytech.powerdatacollect.core.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @Author zay
 * @Date 2025/12/30 10:27
 */
@Configuration
public class OkHttpConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)   // 连接超时
                .readTimeout(30, TimeUnit.SECONDS)      // 读取超时
                .writeTimeout(30, TimeUnit.SECONDS)     // 写入超时
                .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES)) // 连接池：20个连接，5分钟空闲超时
                .retryOnConnectionFailure(true)         // 失败重试
                .build();
    }
}
