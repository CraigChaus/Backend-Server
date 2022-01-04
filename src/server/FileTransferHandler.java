package server;

import client.ClientHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class FileTransferHandler extends Thread{
    Socket socket;
    InputStream inputStream;
    FileOutputStream fileOutputStream;
    BufferedOutputStream bufferedOutputStream;
    OutputStream outputStream;
    int bufferSize;


    FileTransferHandler(Socket client,OutputStream outputStream) {
        this.socket = client;
        inputStream = null;
        fileOutputStream = null;
        bufferedOutputStream = null;
        bufferSize = 0;
        this.outputStream = outputStream;
    }

    @Override
    public void run() {

        PrintWriter printWriter = new PrintWriter(outputStream);
        //TODO: do something here lol


    }

    void receiveFile(String fileName) {
        try {
            inputStream = socket.getInputStream();
            bufferSize = socket.getReceiveBufferSize();
            System.out.println("Buffer size: " + bufferSize);
            fileOutputStream = new FileOutputStream(fileName);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            byte[] bytes = new byte[bufferSize];
            int count;
            while ((count = inputStream.read(bytes)) >= 0) {
                bufferedOutputStream.write(bytes, 0, count);
            }
            bufferedOutputStream.close();
            inputStream.close();
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
            bufferedOutputStream1 = new BufferedOutputStream(socket.getOutputStream());
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
