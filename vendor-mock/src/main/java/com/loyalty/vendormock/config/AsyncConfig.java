package com.loyalty.vendormock.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration
public class AsyncConfig {
    // Uses Spring's default SimpleAsyncTaskExecutor.
    // For production-grade, replace with a ThreadPoolTaskExecutor bean.
}
