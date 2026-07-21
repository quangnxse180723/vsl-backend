package com.vslbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Cau hinh thread pool rieng biet cho tac vu AI (cat video + ONNX inference).
 *
 * Tai sao can pool rieng:
 *   - ONNX inference mat 2-5 giay / request.
 *   - Neu chay tren Tomcat thread (moi request 1 thread), 200 request dong thoi
 *     se lam can kiet pool cua Tomcat, liet toan bo cac API con lai.
 *   - Voi aiTaskExecutor, Tomcat nha thread ngay lap tuc; AI queue xu ly dan.
 *
 * Thong so:
 *   corePoolSize  = 4  : luon giu 4 thread san sang (phu hop server 4 vCPU).
 *   maxPoolSize   = 8  : burst len toi 8 thread khi hang doi day.
 *   queueCapacity = 50 : toi da 50 request cho trong hang doi.
 *   Khi queueCapacity bi vuot qua: RejectedExecutionException -> 503 Service Unavailable.
 */
@Slf4j
@Configuration
@EnableAsync
@org.springframework.scheduling.annotation.EnableScheduling
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-executor-");
        executor.setKeepAliveSeconds(60);

        // Khi hang doi day va maxPoolSize da dat: log va nem loi 503
        executor.setRejectedExecutionHandler((runnable, pool) -> {
            log.error("AI task queue full (maxPool={}, queueCapacity=50). Request rejected.",
                    pool.getMaximumPoolSize());
            throw new RejectedExecutionException("He thong dang qua tai, vui long thu lai sau");
        });

        executor.initialize();
        log.info("aiTaskExecutor initialized: core={}, max={}, queue=50", 4, 8);
        return executor;
    }
}
