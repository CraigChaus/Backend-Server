package fileHandler;

import clientHandler.ClientHandler;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ClientFileHandler extends Thread{

    private ClientHandler sender;
    private ClientHandler receiver;
    private Socket fileSocket;

    public ClientFileHandler(ClientHandler sender, ClientHandler receiver, Socket fileSocket) {
        System.out.println("New ClientFileHandler thread is started: " + this.getName());
        this.sender = sender;
        this.receiver = receiver;
        this.fileSocket = fileSocket;
    }

    @Override
    public void run() {
        if (!fileSocket.isClosed()) {
            try {
                DataInputStream dataInputStream = new DataInputStream(fileSocket.getInputStream());

//                int filenameLength = dataInputStream.readInt();
//
//                if (filenameLength > 0) {
//                    byte[] filenameBytes = new byte[filenameLength];
//                    dataInputStream.readFully(filenameBytes, 0, filenameBytes.length);
//                    String filename = new String(filenameBytes);

                    System.out.println("Receiver of the file is: " + receiver);

                    int fileLength = dataInputStream.readInt();

                    System.out.println("File length: " + fileLength);

                    if (fileLength > 0) {
                        byte[] fileBytes = new byte[fileLength];
                        System.out.println("File bytes length: " + fileBytes.length);
                        dataInputStream.readFully(fileBytes, 0, fileBytes.length);

                        System.out.println("File bytes: " + Arrays.toString(fileBytes));
//                        System.out.println("Filename: " + filename);
                        System.out.println("Text of the file: " + new String(fileBytes));

                        // Send file to receiver
                        receiver.sendFile(fileBytes);
                    } else {
                        System.out.println("File bytes length is 0");
                    }
//                }

            } catch (IOException e) {
                try {
                    fileSocket.close();
                    System.out.println("File socket is closed");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }

        System.out.println("FileSocket is closed! Line 52");
    }



}
