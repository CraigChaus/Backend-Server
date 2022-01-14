package clientHandler;

import server.FileTransferHandler;
import server.ChatServer;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandler extends Thread {
    private Statuses status;
    private String username;
    private String password;


    private ChatServer chatServer;
    private FileTransferHandler fileTransferHandler;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader serverReader;
    private PrintWriter writer;
    private String receivedMessage;

    public ClientHandler(Socket socket, ChatServer chatServer) {
        this.status = null;
        this.username = "";
        this.password = "";
        this.socket = socket;
        this.chatServer = chatServer;
    }

    @Override
    public void run() {
        while (true) {
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                serverReader = new BufferedReader(new InputStreamReader(inputStream));
                writer = new PrintWriter(outputStream);

                receivedMessage = serverReader.readLine();

                if (receivedMessage.equals("PONG")) {
                    System.out.println("<<<< PONG");
                } else {
                    processMessage(receivedMessage);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void processMessage(String message) throws IOException {
        String[] command = parseMessage(message);

        switch (command[0]) {
            case "CONN":
                String username = command[1];

                boolean isUsernameAcceptable = checkUsername(username);

                if (!isUsernameAcceptable) {
                    writeToClient("ERR02 Username has an invalid format (only characters, numbers and underscores are allowed)");
                } else {
                    chatServer.loginUser(this, username);
                }
                break;

            case "BCST":

                if (checkIfLoggedIn()) {
                    String userMessage = command[1];

                    chatServer.sendBroadcastToEveryone(this, userMessage);
                }

                break;

            case "LST":

                if (checkIfLoggedIn()) {
                    chatServer.listAllClients(this);
                }

                break;

            case "GRP CRT":
                if (checkIfLoggedIn()) {
                    chatServer.createGroup(command[1], this);
                }

                break;

            case "GRP JOIN":
                if (checkIfLoggedIn()) {
                    chatServer.joinGroup(command[1], this);
                }

                break;

            case "GRP LST":
                if (checkIfLoggedIn()) {
                    chatServer.listAllGroups(this);
                }

                break;

            case "GRP BCST":
                if (checkIfLoggedIn()) {
                    chatServer.sendBroadcastToGroup(this, command[1], command[2]);
                }
                break;

            case "GRP EXIT":
                if (checkIfLoggedIn()) {
                    chatServer.leaveGroupChat(command[1], this);
                }
                break;

            case "PMSG":
                if (checkIfLoggedIn()) {
                    System.out.println("Recieved PMSG");
                    chatServer.sendPrivateMessage(this, command[1], command[2]);
                }
                break;

            case "FIL ACK":
                if(checkIfLoggedIn()){
                    chatServer.sendAcknowledgement(this, command[1],command[2]);
                }
                break;
            case "FIL SND":
                if(checkIfLoggedIn()){
                    //TODO: implement sending file
                   fileTransferHandler.sendFile(command[3]);
                }
                break;

            case "ACC":
                chatServer.respondToAck(this, message.split(" ")[1], "ACC");
                fileTransferHandler.receiveFile(command[1]);
                break;

            case "DEC":
                chatServer.respondToAck(this, message.split(" ")[1], "DEC");
                break;

            case "PASS":
                try {
                    chatServer.createPassword(message.split(" ")[1], this);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    e.printStackTrace();
                }
                break;

            case "AUTH":
                chatServer.authenticateMe(message.split(" ")[1], this);
                break;
        }
    }

    public String[] parseMessage(String message) {
        String command = message.split(" ")[0];
        //The first element of the array is command, the second is message
        String[] commandAndMessage = new String[]{};

        switch (command) {
            case "GRP":
                if (message.split(" ")[1].equals("BCST")) {
                    String[] splitMessage = message.split(" ", 4);
                    commandAndMessage = new String[]{splitMessage[0] + " " + splitMessage[1], splitMessage[2], splitMessage[3]};
                } else if (!message.split(" ")[1].equals("LST")) {
                    commandAndMessage = new String[]{message.split(" ")[0] + " " + message.split(" ")[1]
                            , message.split(" ", 3)[2]};
                } else {
                    commandAndMessage = new String[]{message.split(" ")[0] + " " + message.split(" ")[1]};
                }

                break;

            case "PMSG":
                String[] splitMessage = message.split(" ", 3);
                commandAndMessage = new String[]{splitMessage[0], splitMessage[1], splitMessage[2]};
                break;

            case "FIL":
                if (message.split(" ")[1].equals("ACK")) {
                    String[] splitMessageAck = message.split(" ");
                    commandAndMessage = new String[]{splitMessageAck[0] + " " + splitMessageAck[1], splitMessageAck[2]};
                } else if (message.split(" ")[1].equals("SND")) {
                    String[] splitMessageSnd = message.split(" ");
                    commandAndMessage = new String[]{splitMessageSnd[0] + " " + splitMessageSnd[1],splitMessageSnd[2],splitMessageSnd[3]};
                }

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

    public boolean checkIfLoggedIn() {
        if (status.equals(Statuses.LOGGED_IN)) {
            return true;
        } else {
            writeToClient("ERR03 Please log in first");
            return false;
        }
    }


    public void getFile(String path) throws IOException {
     byte[] bytes = new byte[10000];

     Socket fileSocket = fileTransferHandler.getFileSocket();
     InputStream inputStream = fileSocket.getInputStream();
     FileOutputStream fileOutputStream = new FileOutputStream(path);

     inputStream.read(bytes,0, bytes.length);
     fileOutputStream.write(bytes,0, bytes.length);
    }



    public boolean checkIfAuthenticated() {
        return status.equals(Statuses.AUTHENTICATED);
    }

    public Statuses getStatus() {
        return status;
    }

    public void setStatus(Statuses status) {
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

    public PrintWriter getWriter() {
        return writer;
    }
}
