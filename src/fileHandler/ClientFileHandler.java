package fileHandler;

import clientHandler.ClientHandler;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ClientFileHandler extends Thread{

    private ClientHandler clientHandler;
    private Socket fileSocket;

    public ClientFileHandler(ClientHandler clientHandler, Socket fileSocket) {
        System.out.println("New ClientFileHandler thread is started");
        this.clientHandler = clientHandler;
        this.fileSocket = fileSocket;
    }

    @Override
    public void run() {
        while (!fileSocket.isClosed()) {
            try {
                DataInputStream dataInputStream = new DataInputStream(fileSocket.getInputStream());
                int receiverNameLength = dataInputStream.readInt();
                System.out.println("Received name length: " + receiverNameLength);

                if (receiverNameLength > 0) {
                    byte[] receiverBytes = new byte[receiverNameLength];
                    System.out.println("Receiver bytes length: " + receiverBytes.length);
                    dataInputStream.readFully(receiverBytes,0, receiverBytes.length);
                    System.out.println("Receiver bytes: " + Arrays.toString(receiverBytes));


                    System.out.println("Bro 3");
                    String receiver = new String(receiverBytes);

                    System.out.println("Receiver of the file is: " + receiver);

                    int fileLength = dataInputStream.readInt();

                    System.out.println("File length: " + fileLength);

                    if (fileLength > 0) {
                        byte[] fileBytes = new byte[fileLength];
                        System.out.println("File bytes length: " + fileBytes.length);
                        dataInputStream.readFully(fileBytes,0, fileBytes.length);

                        System.out.println("File bytes: " + Arrays.toString(fileBytes));
                        System.out.println("Text of the file: " + new String(fileBytes));

                        clientHandler.transferFile(receiver, fileBytes);
                    } else {
                        System.out.println("File bytes length is 0");
                    }

                } else {
                    System.out.println("Receiver bytes length is 0");
                }
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
