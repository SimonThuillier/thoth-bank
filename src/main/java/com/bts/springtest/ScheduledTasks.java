package com.bts.springtest;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduledTasks implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public ScheduledTasks() {
        System.out.println("creating scheduled tasks");
    }

    public void run() {
        System.out.println(dateFormat.format(new Date()));
        log.info("The time is now {}", dateFormat.format(new Date()));
    }
}
