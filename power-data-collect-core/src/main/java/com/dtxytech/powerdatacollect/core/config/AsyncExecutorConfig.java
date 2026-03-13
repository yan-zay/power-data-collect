package com.dtxytech.powerdatacollect.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 * 用于处理小型异步业务请求
 * @Author zay
 * @Date 2026/3/13 14:59
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncExecutorConfig {

    /**
     * 小型异步业务线程池
     * 核心参数：
     * - corePoolSize: 2 (核心线程数，保持活跃)
     * - maxPoolSize: 5 (最大线程数，应对突发请求)
     * - queueCapacity: 20 (队列容量，缓冲小请求)
     * - keepAliveSeconds: 60 (非核心线程存活时间)
     */
    @Bean("queryBusinessExecutor")
    public Executor queryBusinessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：保持 2 个线程常驻
        executor.setCorePoolSize(2);
        // 最大线程数：最多 5 个线程
        executor.setMaxPoolSize(5);
        // 队列容量：最多缓存 20 个任务
        executor.setQueueCapacity(20);
        // 线程名称前缀
        executor.setThreadNamePrefix("query-async-business-data");
        // 线程空闲时间（秒）：非核心线程 60 秒后回收
        executor.setKeepAliveSeconds(60);
        // 拒绝策略：队列满且达到最大线程数时，由调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间（秒）：关闭时最多等待 30 秒
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("QueryBusinessExecutor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }
}
