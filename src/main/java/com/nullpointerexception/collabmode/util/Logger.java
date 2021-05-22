package com.nullpointerexception.collabmode.util;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

import java.io.File;

public class Logger {
    private File logFile;

    public Logger(String logFilePath){
        this.logFile = new File(logFilePath);
    }

    public Logger(){
        //TODO:
    }

    public void log(String tag, String message){
        final SimpleDateFormat timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String prefix = "[%s] [%s @ %s (%s)]: ";
        prefix = String.format(prefix, timestamp, tag, Thread.currentThread().getName(), Thread.currentThread().getId());
        try {
            FileUtils.writeStringToFile(
                    logFile,
                    prefix + message + "\n",
                    StandardCharsets.UTF_8, true);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
