package com.bts.thoth.bank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "projection-data-source")
@PropertySource(value = "classpath:app-config.properties")
public class ProjectionDataSourceConfig extends DataSourceConfig {
}
