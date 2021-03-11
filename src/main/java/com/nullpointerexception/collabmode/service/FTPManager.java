package com.nullpointerexception.collabmode.service;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.net.ConnectException;

public class FTPManager {
    private static FTPClient ftpClient = null;

    public FTPManager(String serverAddress, int serverPort, String ftpUser, String ftpPassword){
        ftpClient = new FTPClient();
        try {
            ftpClient.connect(serverAddress, serverPort);
            int serverReplyCode = ftpClient.getReplyCode();
            if(!FTPReply.isPositiveCompletion(serverReplyCode)){
                throw new ConnectException("FTP Server connection can not be established. (Code: " + serverReplyCode + ")");
            }
            boolean success = ftpClient.login(ftpUser, ftpPassword);
            if (!success) {
                throw new ConnectException("Login in the FTP Server failed");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FTPFile[] getFiles(){
        if(ftpClient != null){
            try {
                FTPFile[] files = ftpClient.listFiles();
                return files;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public FTPFile[] getFilesByPath(String path){
        if(ftpClient != null){
            try {
                FTPFile[] files = ftpClient.listFiles(path);
                return files;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void logout(){
        if(ftpClient != null) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean addNewDirectory(String directoryName){
        if(ftpClient != null) {
            try {
                return ftpClient.makeDirectory(directoryName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

}
