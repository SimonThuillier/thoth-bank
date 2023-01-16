package com.bts.springtest;

import java.time.Duration;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import static io.vavr.API.println;

@SpringBootApplication

public class SpringTest {

    public static void main(String[] args) {

        ApplicationContext context = new AnnotationConfigApplicationContext(
                Config.class, FrodonConfig.class);

        println(context.getBean("lol"));
        println(context.getEnvironment().getProperty("lol"));

        println(context.getEnvironment().getProperty("logging.file"));
        println(context.getEnvironment().getProperty("logging.level.root"));

        println(context.getEnvironment().getProperty("spring.jmx.enabled"));
        println(context.getEnvironment().getProperty("objectvar.arrayvar"));
        println(context.getEnvironment().getProperty("objectvar.arrayvar[0]"));

        println(context.getEnvironment().getProperty("sam"));
        println(context.getEnvironment().getProperty("frodon"));
        println(context.getEnvironment().getProperty("tolkien"));

        println(context.getBean("frodon.sam"));

        println(context.getEnvironment().toString());

        Company company = context.getBean("company", Company.class);

        TaskExecutor executor = context.getBean("taskExecutor", TaskExecutor.class);
        TaskScheduler scheduler = context.getBean("taskScheduler", TaskScheduler.class);

        /*SpringApplication.run(InputMultiplier.class);
        SpringApplication.run(ScheduledTasks.class);*/
        // executor.execute(new InputMultiplier());

        executor.execute(new InputMultiplier());
        scheduler.schedule(new ScheduledTasks(),new PeriodicTrigger(Duration.ofSeconds(5L) ));
    }


}