package com.grocery.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// Dedicated thread pool for the actual WhatsApp/email sends fired by
// ScheduledSendService, kept separate from Spring's default 1-thread
// @Scheduled pool so a slow/hung send can never block the per-minute
// checkAndSend() run for other users.
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

	@Bean(name = "scheduledSendExecutor")
	public Executor scheduledSendExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("scheduled-send-");
		executor.initialize();
		return executor;
	}

	@Override
	public Executor getAsyncExecutor() {
		return scheduledSendExecutor();
	}
}
