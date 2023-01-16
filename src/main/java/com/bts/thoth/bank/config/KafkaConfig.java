package com.bts.thoth.bank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "kafka")
@PropertySource(value = "classpath:app-config.properties")
public class KafkaConfig {
    private String host;
    private String topic;
    private String callbackTopic;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getCallbackTopic() {
        return callbackTopic;
    }

    public void setCallbackTopic(String callbackTopic) {
        this.callbackTopic = callbackTopic;
    }
}
