package com.truerize.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${app.mail.executor.core-pool-size:4}")
    private int corePoolSize;

    @Value("${app.mail.executor.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${app.mail.executor.queue-capacity:5000}")
    private int queueCapacity;

    @Value("${app.mail.executor.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Value("${app.mail.executor.await-termination-seconds:300}")
    private int awaitTerminationSeconds;

    @Bean(name = { "mailTaskExecutor", "taskExecutor" })
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("MailAsync-");
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        log.info(
            "Async mail executor initialized: core={}, max={}, queue={}, keepAlive={}s, awaitTermination={}s",
            executor.getCorePoolSize(),
            executor.getMaxPoolSize(),
            executor.getQueueCapacity(),
            keepAliveSeconds,
            awaitTerminationSeconds
        );

        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error(
            "Async method {} failed with {} parameter(s): {}",
            method.getName(),
            params != null ? params.length : 0,
            ex.getMessage(),
            ex
        );
    }
}
