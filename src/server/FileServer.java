package server;

import client.ClientHandler;
import client.Statuses;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class FileServer {

    InputStream inputStream;
    OutputStream outputStream;
    ArrayList<FileTransferHandler> fileTransferHandlers;

    public FileServer(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }
    public void startFileServer() throws IOException {
        var serverFileSocket = new ServerSocket(1338);

        while (true) {
            // Wait for an incoming client-connection request (blocking).
            Socket socket = serverFileSocket.accept();

            // Your code here:
            // TODO: Start a message processing and file thread for each connecting client.
           FileTransferHandler fileTransferHandler = new FileTransferHandler(socket,this);
           fileTransferHandler.start();
        }
    }

    public void fillInUsername(FileTransferHandler fileTransferHandlerName,String username){
        boolean usernameExists = false;

        for (FileTransferHandler fileTransferHandler: fileTransferHandlers) {
            if (username.equalsIgnoreCase(fileTransferHandler.getUsername())) {
                usernameExists = true;
            }
        }

        if (!usernameExists) {
            fileTransferHandlerName.setName(username);
            fileTransferHandlers.add(fileTransferHandlerName);

        } else{
            System.out.println("User already exist, system error");
        }
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    //TODO: check this out tomorrow
    public void sendToClient(String username,File filePath){
        for (FileTransferHandler fileTransferHandler:fileTransferHandlers) {
            if(fileTransferHandler.getUsername().equals(username)){

                    FileInputStream fileInputStream;
                    BufferedInputStream bufferedInputStream;
                    BufferedOutputStream bufferedOutputStream1;

                    byte[] buffer = new byte[8192];
                    try {
                        fileInputStream = new FileInputStream(filePath);
                        bufferedInputStream = new BufferedInputStream(fileInputStream);
                        bufferedOutputStream1 = new BufferedOutputStream(outputStream);
                        int count;
                        while ((count = bufferedInputStream.read(buffer)) > 0) {
                            bufferedOutputStream1.write(buffer, 0, count);
                        }
                        bufferedOutputStream1.close();
                        fileInputStream.close();
                        bufferedInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
    }


}
