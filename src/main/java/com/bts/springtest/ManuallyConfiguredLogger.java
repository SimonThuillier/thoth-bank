package com.bts.springtest;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * abstract class providing a singleton logger and utilitaries methods to handle other loggers
 * use of this class allows for using a singleton BankLogger writing to {.env.LOG_FILEPATH}/ManuallyConfiguredLogger.log
 * Also it redirects
 */
public abstract class ManuallyConfiguredLogger {
    // using singleton pattern
    private static final String name = "ManuallyConfiguredLogger";
    private static final Logger instance;
    private static final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    private static final RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
    private static final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();

    // static block initialization for exception handling
    static {
        try {


            // Create a PatternLayoutEncoder
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(loggerContext);
            encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n");
            encoder.start();
            // Set the encoder for the FileAppender
            fileAppender.setEncoder(encoder);

            // Create a TimeBasedRollingPolicy and set it as policy for the fileAppender
            rollingPolicy.setContext(loggerContext);
            rollingPolicy.setParent(fileAppender);
            rollingPolicy.setFileNamePattern("/home/simon/projects/kafka/thoth-bank/logs/" + "/" + name + ".log.%d{yyyy-MM-dd}");
            rollingPolicy.start();

            fileAppender.setFile("/home/simon/projects/kafka/thoth-bank/logs/" + "/" + name + ".log");
            fileAppender.setRollingPolicy(rollingPolicy);

            fileAppender.setContext(loggerContext);

            fileAppender.start();



            instance = LoggerFactory.getLogger(name);
            configureLogger(name);
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred in creating BankLogger: " + e.getMessage());
        }
    }

    public static void configureLogger(String loggerName){
        for (Iterator<Appender<ILoggingEvent>> it = loggerContext.getLogger(loggerName).iteratorForAppenders(); it.hasNext(); ) {
            Appender appender = it.next();
            loggerContext.getLogger(loggerName).detachAppender(appender);
        }
        loggerContext.getLogger(loggerName).addAppender(fileAppender);
    }

    public static void configureLoggers(List<String> loggerNames){
        for(String loggerName: loggerNames){
            ManuallyConfiguredLogger.configureLogger(loggerName);
        }
    }

    public static void configureAllLoggers(){
        ManuallyConfiguredLogger.configureLoggers(
                loggerContext
                        .getLoggerList()
                        .stream()
                        .map(ch.qos.logback.classic.Logger::getName)
                        .collect(Collectors.toList()
                        ));
    }

    public static Logger get() {
        return instance;
    }
}