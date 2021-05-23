package com.nullpointerexception.collabmode.util;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

import java.io.File;
import java.util.Date;

public class Logger {
    private File logFile;

    public Logger(String logFilePath){
        this.logFile = new File(logFilePath);
    }

    public Logger(){
        String dataFolder = System.getenv("APPDATA");
        int logCounter = 1;
        SimpleDateFormat timestamp = new SimpleDateFormat("yyyy-MM-dd");
        logFile = new File(dataFolder + "\\CollabMode\\logs\\log-" + timestamp.format(new Date()) + "-" + logCounter + ".txt");
        while(logFile.exists()){
            logCounter++;
            logFile = new File(dataFolder + "\\CollabMode\\logs\\log-" +timestamp.format(new Date()) + "-" + logCounter + ".txt");
        }
        try {
            logFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(String tag, String message){
        final SimpleDateFormat timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String prefix = "[%s] [%s @ %s (%s)]: ";
        prefix = String.format(prefix, timestamp.format(new Date()), tag, Thread.currentThread().getName(), Thread.currentThread().getId());
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
