package com.luluroute.ms.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class SpringAsyncConfig {

    @Value("${shipmentresponse.async.corePoolSize}")
    private Integer corePoolSize;
    @Value("${shipmentresponse.async.maxPoolSize}")
    private Integer maxPoolSize;

    @Bean(name = "SvcMessageExecutor")
    public Executor createSvcMessageExecutor() {
        ThreadPoolTaskExecutor svcMessageExecutor = new ThreadPoolTaskExecutor();
        svcMessageExecutor.setCorePoolSize(corePoolSize);
        svcMessageExecutor.setMaxPoolSize(maxPoolSize);
        svcMessageExecutor.setKeepAliveSeconds(20);
        svcMessageExecutor.setAllowCoreThreadTimeOut(true);
        svcMessageExecutor.initialize();
        return svcMessageExecutor;
    }

    @Bean
    public Executor svcDBExecutor() {
        ThreadPoolTaskExecutor svcDBExecutor = new ThreadPoolTaskExecutor();
        svcDBExecutor.setCorePoolSize(corePoolSize);
        svcDBExecutor.setMaxPoolSize(maxPoolSize);
        svcDBExecutor.setKeepAliveSeconds(20);
        svcDBExecutor.setAllowCoreThreadTimeOut(true);
        svcDBExecutor.initialize();
        return svcDBExecutor;
    }
}
