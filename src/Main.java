import client.ClientHandler;
import server.Server;

import java.io.*;

public class Main {
    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.startServer();
    }
}
