package com.nullpointerexception.collabmode.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;

public class Serializer {
    private final String FILE_NAME = "auth";
    private final String FILE_EXTENSION = ".ser";
    private  String LOCAL_APP_DATA_DIR = "";

    public Serializer(){
        LOCAL_APP_DATA_DIR = System.getenv("APPDATA") + "\\CollabMode\\";
    }

    public void serializeToken(String token) throws IOException {
        Path p = Paths.get(LOCAL_APP_DATA_DIR + FILE_NAME + FILE_EXTENSION);
        FileOutputStream fis = new FileOutputStream(p.toString());
        DosFileAttributes dos = Files.readAttributes(p, DosFileAttributes.class);
        Files.setAttribute(p, "dos:hidden", true);
        ObjectOutputStream oos = new ObjectOutputStream(fis);
        oos.writeObject(token);
    }

    public String deserializeToken() throws IOException, ClassNotFoundException {
        Path p = Paths.get(LOCAL_APP_DATA_DIR + FILE_NAME + FILE_EXTENSION);
        FileInputStream fis=new FileInputStream(p.toString());
        ObjectInputStream ois=new ObjectInputStream(fis);
        return (String) ois.readObject();
    }
}
