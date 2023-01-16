package com.bts.springtest;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class ShutdownHookBean implements ApplicationListener<ContextClosedEvent> {

    private ConfigurableApplicationContext ctx;

    public ShutdownHookBean(ConfigurableApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        System.out.println("Something went wrong !");
        ctx.close();
        SpringApplication.exit(ctx, () -> 127);
        System.exit(127);
        System.out.println("lol");
    }
}

class TerminateBean {

    @PreDestroy
    public void onDestroy() throws Exception {
        System.out.println("Spring Container is destroyed!");
    }
}
