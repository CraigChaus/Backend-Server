import server.ChatServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        ChatServer chatServer = new ChatServer();
        System.out.println("Chat Server started");
        chatServer.startServer();
    }
}
