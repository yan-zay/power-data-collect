package com.dtxytech.powerdatacollect.core.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * @Author zay
 * @Date 2025/12/30 10:27
 */
@Configuration
public class OkHttpConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        // 创建信任所有证书的TrustManager（仅用于测试环境，生产环境应正确配置SSL）
        X509TrustManager trustAllCerts = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }
        };

        // 创建一个不验证主机名的HostnameVerifier
        HostnameVerifier hostnameVerifier = (hostname, session) -> true;

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllCerts}, null);

            return new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)   // 连接超时
                    .readTimeout(30, TimeUnit.SECONDS)      // 读取超时
                    .writeTimeout(30, TimeUnit.SECONDS)     // 写入超时
                    .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES)) // 连接池：20个连接，5分钟空闲超时
                    .retryOnConnectionFailure(true)         // 失败重试
                    .sslSocketFactory(sslContext.getSocketFactory(), trustAllCerts) // 设置信任所有证书
                    .hostnameVerifier(hostnameVerifier)   // 设置主机名验证
                    .build();
        } catch (Exception e) {
            // 如果SSL配置失败，回退到基本配置
            return new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)   // 连接超时
                    .readTimeout(30, TimeUnit.SECONDS)      // 读取超时
                    .writeTimeout(30, TimeUnit.SECONDS)     // 写入超时
                    .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES)) // 连接池：20个连接，5分钟空闲超时
                    .retryOnConnectionFailure(true)         // 失败重试
                    .build();
        }
    }
}
