package com.nullpointerexception.collabmode.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FTPManager {
    private static FTPClient ftpClient = null;
    public static final String FTP_SERVER_ADDRESS = "192.168.0.101";

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

    //added
    public boolean rename(String from, String to) {
        boolean success = false;
        if(ftpClient != null) {
            try {
                return success = ftpClient.rename(from, to);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean deleteFile(String pathname){
        boolean deleted;
        if(ftpClient != null) {
            try {
                return deleted = ftpClient.deleteFile(pathname);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean uploadFile(String pathname, String pathOnServer){
        boolean uploaded;
        if(ftpClient != null){
            try {
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                File uploadFile = new File(pathname);
                Path uploadFilePath = Paths.get(pathname);
                String uploadedFile = uploadFilePath.getFileName().toString();
                if(FilenameUtils.getExtension(uploadedFile).equals("tmp")){
                    int p = uploadedFile.lastIndexOf('.');
                    if (p>0) {
                        uploadedFile = uploadedFile.substring(0, p);
                    }
                }

                InputStream inputStream = new FileInputStream(uploadFile);
                uploadedFile = pathOnServer + "/" + uploadedFile;
                System.out.println(pathOnServer + "/" + uploadedFile);
                System.out.println("Start uploading first file");
                uploaded = ftpClient.storeFile(uploadedFile, inputStream);
                inputStream.close();
                return uploaded;


            }catch (IOException e){
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean downloadFile(String pathname){
        boolean downloaded;
        if(ftpClient != null){
            try {
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                String tempFolder = System.getProperty("java.io.tmpdir");
                Path tempFolderPath = Paths.get(tempFolder + "\\.collabmode");
                File downloadedFile = new File(tempFolderPath.toString() + "\\" + Paths.get(pathname).getFileName());
                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(downloadedFile));
                downloaded = ftpClient.retrieveFile(pathname, outputStream);
                outputStream.close();
                return downloaded;

            }catch (IOException e){
                e.printStackTrace();
            }
        }
        return false;
    }




}
