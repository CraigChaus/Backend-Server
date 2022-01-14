import server.ChatServer;

import java.io.*;

public class Main {
    public static void main(String[] args) throws IOException {
        ChatServer chatServer = new ChatServer();
        chatServer.startServer();
        System.out.println("Chat Server started");
    }
}
