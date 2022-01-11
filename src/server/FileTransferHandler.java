package server;

import client.ClientHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FileTransferHandler extends Thread{
    private FileServer fileServer;
    private String username;
    private Socket fileSocket;
    private InputStream inputStream;
    private FileOutputStream fileOutputStream;
    private FileInputStream fileInputStream;
    private OutputStream outputStream;
    int bufferSize;

    private boolean transferFinished;

    public FileTransferHandler(Socket fileSocket, FileServer fileServer) throws IOException {
        this.fileServer = fileServer;
        this.fileSocket = fileSocket;
        this.username = username;

        bufferSize = 0;
    }

    @Override
    public void run() {
        while (true) {
            try {

                inputStream = fileSocket.getInputStream();
                outputStream = fileSocket.getOutputStream();

                //check this line
                bufferSize = fileSocket.getReceiveBufferSize();
                fileServer.fillInUsername(this,username);
                System.out.println("CLient "+ username + " has connected");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }




    void receiveFile(String filePath) {
        try {

            System.out.println("Buffer size: " + bufferSize);
            fileInputStream = new FileInputStream(filePath);
            outputStream = fileSocket.getOutputStream();

           // fileOutputStream = new FileOutputStream(fileName);
          //  bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

            byte[] bytes = new byte[10000];
            fileInputStream.read(bytes,0,bytes.length);
            outputStream.write(bytes,0, bytes.length);

           // int count;
           // while ((count = inputStream.read(bytes)) >= 0) {
          //      bufferedOutputStream.write(bytes, 0, count);
           // }
         //   bufferedOutputStream.close();
          //  inputStream.close();

            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendFile(File file) {

        FileInputStream fileInputStream;
        BufferedInputStream bufferedInputStream;
        BufferedOutputStream bufferedOutputStream1;

        byte[] buffer = new byte[8192];
        try {
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            bufferedOutputStream1 = new BufferedOutputStream(fileSocket.getOutputStream());
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

    public Socket getFileSocket() {
        return fileSocket;
    }

    public FileServer getFileServer() {
        return fileServer;
    }

    public String getUsername() {
        return username;
    }
}
