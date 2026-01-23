package com.dtxytech.powerdatacollect.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author zay
 * @Date 2026/1/21 15:29
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    @Bean(name = "dataSyncExecutor")
    public ThreadPoolExecutor dataSyncExecutor() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        // 根据CPU核心数和I/O密集型任务调整
        int corePoolSize = Math.min(cpuCores, 8);  // I/O密集型任务可以适当增加
        int maxPoolSize = Math.min(cpuCores * 2, 16); // 最大线程数
        log.info("DataSyncThreadPool, cpuCores:{}, corePoolSize: {}, maxPoolSize: {}", cpuCores, corePoolSize, maxPoolSize);

        return new ThreadPoolExecutor(
                corePoolSize,      // 核心线程数
                maxPoolSize,       // 最大线程数
                60L,               // 空闲线程存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200), // 增加队列容量
                new ThreadFactory() {
                    private int counter = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "DataSyncThread-" + counter++);
                        t.setDaemon(false);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
    }
}
