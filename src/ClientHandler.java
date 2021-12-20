import java.io.*;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandler extends Thread {
    private String status;
    private String username;
    private String password;


    private Server server;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader serverReader;
    private PrintWriter writer;

    public ClientHandler(Socket socket, Server server) {
        this.status = null;
        this.username = "";
        this.password = " ";
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        while (true) {
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                serverReader = new BufferedReader(new InputStreamReader(inputStream));
                writer = new PrintWriter(outputStream);

                String receivedMessage = serverReader.readLine();

                processMessage(receivedMessage);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void processMessage(String message) {
        String[] command = parseMessage(message);

        switch (command[0]) {
            case "CONN":
                String username = command[1];

                boolean isUsernameAcceptable = checkUsername(username);

                if (!isUsernameAcceptable) {
                    writeToClient("ERR02 Username has an invalid format (only characters, numbers and underscores are allowed)");
                } else {
                    server.loginUser(this, username);
                }
                break;

            case "BCST":

                if (status.equals("CONNECTED")) {
                    String userMessage = command[1];

                    server.sendBroadcastToEveryone(this, userMessage);
                } else {
                    writeToClient("ERR03 Please log in first");
                }
        }
    }

    public String[] parseMessage(String message) {
        String command = message.split(" ")[0];
        //The first element of the array is command, the second is message
        String[] commandAndMessage;

        switch (command) {
            case "GRP":
                commandAndMessage = new String[]{message.split(" ")[0] + " " + message.split(" ")[1]
                        , message.split(" ", 3)[2]};

                break;
            default:
                commandAndMessage = new String[]{command, message.split(" ", 2)[1]};
                break;
        }
        return commandAndMessage;
    }

    public boolean checkUsername(String username) {
        Pattern pattern = Pattern.compile("[- !@#$%^&*()+=|/?.>,<`~]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(username);

        // Return true if there are no characters from the list above, and returns false, if the match is found
        return !matcher.find();
    }

    public void writeToClient(String message) {
        writer.println(message);
        writer.flush();
    }




    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
