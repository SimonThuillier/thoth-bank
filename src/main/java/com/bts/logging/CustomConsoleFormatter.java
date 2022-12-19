package com.bts.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;


public class CustomConsoleFormatter extends Formatter {
    public String format(LogRecord record){
        StringBuilder strb = new StringBuilder(32);
        strb.append(record.getLevel());
        strb.append(" : ");
        strb.append(record.getMessage());
        strb.append("\n");
        return strb.toString();
    }
}