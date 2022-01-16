package server;

import clientHandler.ClientHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class FileServer {

    ArrayList<FileTransferHandler> fileTransferHandlers;
    Socket fileSocket;


    public FileServer() {
        fileTransferHandlers = new ArrayList<>();
    }

    public void startFileServer() throws IOException {
        var serverFileSocket = new ServerSocket(1338);
        fileSocket = new Socket("127.0.0.1",1338);
        while (true) {
            // Wait for an incoming client-connection request (blocking).
            Socket socket = serverFileSocket.accept();

        }
    }

//    public void fillInUsername(String username) throws IOException {
//        boolean usernameExists = false;
//
//        for (FileTransferHandler fileTransferHandler: fileTransferHandlers) {
//            if (username.equalsIgnoreCase(fileTransferHandler.getUsername())) {
//                usernameExists = true;
//            }
//        }
//        FileTransferHandler fileTransferHandlerName = new FileTransferHandler( fileSocket,this);
//
//        if (!usernameExists) {
//            fileTransferHandlerName.setName(username);
//            fileTransferHandlers.add(fileTransferHandlerName);
//
//        } else{
//            System.out.println("User already exist, system error");
//        }
//    }

    //TODO: check this out tomorrow
    public void sendToClient(ClientHandler sender,ClientHandler receive,String fileName){

        // Your code here:
        // TODO: Start a message processing and file thread for each connecting client.
        FileTransferHandler fileTransferHandler = new FileTransferHandler(sender,receive,fileName);
        fileTransferHandler.start();
        fileTransferHandlers.add(fileTransferHandler);

        try {
            fileTransferHandler.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
