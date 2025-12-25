package com.truerize.config;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        
        executor.setCorePoolSize(2);  
        executor.setMaxPoolSize(5);   
        executor.setQueueCapacity(500); 

        executor.setThreadNamePrefix("MailAsync-");
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(300); 
        executor.setAllowCoreThreadTimeOut(true);

       
        executor.setRejectedExecutionHandler(
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.initialize();

        log.info("✅ Async Task Executor initialized: core={}, max={}, queue={}",
            executor.getCorePoolSize(),
            executor.getMaxPoolSize(),
            executor.getQueueCapacity()
        );

        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("❌ Async method {} threw exception: {}",
                method.getName(), ex.getMessage(), ex);
        };
    }
}