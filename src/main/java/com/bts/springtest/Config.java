package com.bts.springtest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
@PropertySource("classpath:/app-config.yml")
@ComponentScan(basePackageClasses = Company.class)
@EnableAsync
@EnableScheduling
class Config {

    @Value("frodon.sam")
    private String frodonSam;

    @Value("${lol}")
    private String lol;

    @Value("${spring}")
    private String springJmx;

    public Config() {

        YamlPropertiesFactoryBean propFactory = new YamlPropertiesFactoryBean();
        // propFactory.setResources(new Resource);

        // this.springConfigPath = springConfigPath;
    }



    @Bean
    public Address getAddress() {
        return new Address("High Street", 1000);
    }

    @Bean
    public String lol() {
        return lol;
    }

    @Bean
    public String springJmx() {
        return springJmx;
    }
}

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("frodon")
@PropertySource("classpath:/app-config.yml")
class FrodonConfig {
    @Value("${sam}")
    private String sam;

    @Bean("frodon.sam")
    public String getSam() {
        return sam;
    }
}