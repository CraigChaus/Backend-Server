package fileHandler;

import java.net.Socket;
import java.util.ArrayList;

public class FileServer {

    ArrayList<ClientFileHandler> clientFileHandlers;
    Socket fileSocket;

    public FileServer() {
        clientFileHandlers = new ArrayList<>();
    }

//    public void startFileServer() throws IOException {
//        var serverFileSocket = new ServerSocket(1338);
////        fileSocket = new Socket("127.0.0.1",1338);
//        while (true) {
//            // Wait for an incoming client-connection request (blocking).
//            Socket fileSocket = serverFileSocket.accept();
//
//            DataInputStream dataInputStream = new DataInputStream(fileSocket.getInputStream());
//
//            int fileLength = dataInputStream.readInt();
//
//            if (fileLength > 0) {
//                byte[] fileBytes = new byte[fileLength];
//                dataInputStream.readFully(fileBytes,0, fileLength);
//            }
//
//        }
//    }

//    public void sendToClient(ClientMessageHandler sender, ClientMessageHandler receive, String fileName){
//
//        // Your code here:
//        // TODO: Start a message processing and file thread for each connecting client.
//        ClientFileHandler clientFileHandler = new ClientFileHandler(sender,receive,fileName);
//        clientFileHandler.start();
//        clientFileHandlers.add(clientFileHandler);
//
//        try {
//            clientFileHandler.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//    }
}
