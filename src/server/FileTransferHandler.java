package server;

import clientHandler.ClientHandler;

import java.io.*;
import java.net.Socket;

public class FileTransferHandler extends Thread{
    private ClientHandler senderClient;
    private ClientHandler receiveClient;
    private InputStream inputStream;
    private FileOutputStream fileOutputStream;
    private FileInputStream fileInputStream;
    private OutputStream outputStream;
    private String filePath;
    int bufferSize;

    private boolean transferFinished;

    public FileTransferHandler(ClientHandler senderClient, ClientHandler receiveClient,String filePath) {
        this.senderClient = senderClient;
        this.receiveClient = receiveClient;
        this.filePath = filePath;
    }

    @Override
    public void run() {

            try {
             sendFile(filePath);

            } catch (IOException e) {
                e.printStackTrace();
            }

    }
//    public void sendFromServerToClient(String fileName){
//        fileServer.sendToClient(fileName);
//    }

//   public void receiveFile(String filePath) {
//        try {
//
//            System.out.println("Buffer size: " + bufferSize);
//            fileInputStream = new FileInputStream(filePath);
//            outputStream = fileSocket.getOutputStream();
//
//           // fileOutputStream = new FileOutputStream(fileName);
//          //  bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
//
//            byte[] bytes = new byte[10000];
//            fileInputStream.read(bytes,0,bytes.length);
//            outputStream.write(bytes,0, bytes.length);
//
//           // int count;
//           // while ((count = inputStream.read(bytes)) >= 0) {
//          //      bufferedOutputStream.write(bytes, 0, count);
//           // }
//         //   bufferedOutputStream.close();
//          //  inputStream.close();
//
//            System.out.println("received file");
//            fileInputStream.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void sendFile(String filePath) throws IOException {
        Socket s = new Socket(System.getProperty("localhost"),1338);

        System.out.println("The file path: " + filePath);
        File file = new File(filePath);

        if(!file.exists()||!file.isFile()){
            System.out.println("ERR File does not exist");
        }

        FileInputStream fileInputStream;
        BufferedInputStream bufferedInputStream;
        BufferedOutputStream bufferedOutputStream1;

        byte[] buffer = new byte[8192];
        try {
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            bufferedOutputStream1 = new BufferedOutputStream(s.getOutputStream());
            int count;
            while ((count = bufferedInputStream.read(buffer)) > 0) {
                bufferedOutputStream1.write(buffer, 0, count);
            }
            System.out.println("FILE SENT "+ filePath);
            bufferedOutputStream1.close();
            fileInputStream.close();
            bufferedInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
