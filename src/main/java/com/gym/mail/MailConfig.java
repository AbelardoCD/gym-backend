package com.gym.mail;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class MailConfig {
    public static final String EXECUTOR = "mailExecutor";

    /** Hilos propios para el correo, separados de los del WebSocket. */
    @Bean(EXECUTOR)
    public ThreadPoolTaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-");
        executor.initialize();
        return executor;
    }
}
