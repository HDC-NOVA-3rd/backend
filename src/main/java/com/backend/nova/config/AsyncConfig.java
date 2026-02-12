package com.backend.nova.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 기본 스레드 수 (평소 대기하는 스레드)
        executor.setCorePoolSize(5);
        // 최대 스레드 수 (트래픽 몰릴 때 늘어나는 최대치)
        executor.setMaxPoolSize(20);
        // 대기열 크기 (모든 스레드가 바쁠 때 대기하는 요청 수)
        executor.setQueueCapacity(50);
        // 스레드 이름 접두사 (로그 볼 때 중요: Push-Async-1, Push-Async-2 ...)
        executor.setThreadNamePrefix("Push-Async-");
        executor.initialize();
        return executor;
    }
}
