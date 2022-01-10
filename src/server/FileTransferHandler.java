package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FileTransferHandler extends Thread{
    private ServerSocket serverFileSocket;

    private Socket fileSocket;
    private InputStream inputStream;
    private FileOutputStream fileOutputStream;
    private BufferedOutputStream bufferedOutputStream;
    private OutputStream outputStream;
    int bufferSize;

    private boolean transferFinished;

    public FileTransferHandler() throws IOException {
        this.serverFileSocket = new ServerSocket(1338);
        bufferSize = 0;
        transferFinished = false;
    }

    @Override
    public void run() {
        while (!transferFinished) {
            try {
                fileSocket = serverFileSocket.accept();

                inputStream = fileSocket.getInputStream();
                outputStream = fileSocket.getOutputStream();
                bufferSize = serverFileSocket.getReceiveBufferSize();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        PrintWriter printWriter = new PrintWriter(outputStream);
        //TODO: do something here lol


    }

    void receiveFile(String fileName) {
        try {

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
}
