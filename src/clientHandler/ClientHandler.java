package clientHandler;

import fileHandler.ClientFileHandler;
import server.ChatServer;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandler extends Thread {
    private Statuses status;
    private String username;
    private String password;
    private boolean encryptionSessionActive;

    private final ChatServer chatServer;

    private final Socket messageSocket;
    private Socket fileSocket;

    private PrintWriter writer;

    public ClientHandler(Socket messageSocket, ChatServer chatServer, Socket fileSocket) {
        this.fileSocket = fileSocket;
        this.status = Statuses.CONNECTED;
        this.username = "";
        this.password = "";
        this.messageSocket = messageSocket;
        this.chatServer = chatServer;

    }

    @Override
    public void run() {
        System.out.println("New user is connected");
        while (!messageSocket.isClosed()) {
            try {

                InputStream inputStream = messageSocket.getInputStream();
                OutputStream outputStream = messageSocket.getOutputStream();

                BufferedReader serverReader = new BufferedReader(new InputStreamReader(inputStream));
                writer = new PrintWriter(outputStream);


                String receivedMessage = serverReader.readLine();

                if (receivedMessage.equals("PONG")) {
//                    System.out.println("<<<< PONG");
                } else {
                    processMessage(receivedMessage);
                }

            } catch (IOException e) {
                try {
                    messageSocket.close();
                    status = Statuses.DISCONNECTED;
                    System.out.println("User " + username + " disconnected");

                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            }
        }

    }

    public void processMessage(String message) throws IOException {
        String[] command = parseMessage(message);

        switch (command[0]) {
            case "CONN":

                if (status == Statuses.CONNECTED || status == Statuses.DISCONNECTED) {
                    String username = command[1];

                    boolean isUsernameAcceptable = checkName(username);

                    if (!isUsernameAcceptable) {
                        writeToClient("ERR02 Name has an invalid format or is empty(only characters, numbers and underscores are allowed)");
                        
                    } else {
                        chatServer.loginUser(this, username);
                    }

                } else {
                    writeToClient("ERR you are already logged in!");
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
                    if (checkName(command[1]))
                        chatServer.createGroup(command[1], this);
                    else
                        writeToClient("ERR02 Name has an invalid format or is empty(only characters, numbers and underscores are allowed)");
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
                    chatServer.sendPrivateMessage(this, command[1], command[2]);
                }
                break;

            case "FIL ACK":
                if(checkIfLoggedIn()){
                    chatServer.sendAcknowledgement(this, command[1],command[2]);

                }
                break;

            case "FIL SND":
                // Send file if logged id
                if(checkIfLoggedIn()){
                    if (checkName(command[3])) {
                        chatServer.sendFileToClient(this, command[1], command[2], command[3]);
                        ClientHandler receiverClient = chatServer.getClientByName(command[1]);
                        new ClientFileHandler(this, receiverClient, fileSocket).start();
                    } else
                        writeToClient("ERR02 Name has an invalid format or is empty(only characters, numbers and underscores are allowed)");
                }

                break;

            case "ACC":
                chatServer.respondToAck(this, message.split(" ")[1], "ACC",message.split(" ")[2]);
                break;

            case "DEC":
                chatServer.respondToAck(this, command[1], "DEC",command[2]);
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

            case "ENC":
                chatServer.forwardClientsPublicKey(this,command[1],command[2]);
                break;

            case "ENCSK":
                chatServer.forwardEncryptedSessionKey(this,command[1],command[2]);
                break;

            case "ENCM":
                chatServer.forwardEncryptedMessageToclient(this,command[1],command[2]);
                break;

            case "QUIT":
                chatServer.disconnectFromTheServer(this);
                messageSocket.close();
                System.out.println("User " + username + " disconnected");
                break;

            default:
                writeToClient("ERR00 Unknown command");
                break;
        }
    }

    public String[] parseMessage(String message) {
        String command = message.split(" ")[0];
        //The first element of the array is command, the second is message
        String[] commandAndMessage = new String[]{};
        String[] payLoad = message.split(" ");

        //TODO: Handle all the commands like you did here
        switch (command) {
            case "GRP":

                switch (payLoad[1]) {
                    case "CRT", "JOIN", "EXIT":
                        commandAndMessage = new String[]{payLoad[0] + " " + payLoad[1], payLoad[2]};
                        break;

                    case "BCST":
                        String[] splitMessage = message.split(" ", 4);
                        commandAndMessage = new String[]{splitMessage[0] + " " + splitMessage[1], splitMessage[2], splitMessage[3]};
                        break;

                    case "LST":
                        commandAndMessage = new String[]{payLoad[0] + " " + payLoad[1]};
                        break;
                }

                break;


            case "PMSG":

                String[] splitMessage = message.split(" ", 3);
                commandAndMessage = new String[]{splitMessage[0], splitMessage[1], splitMessage[2]};
                break;

            case "FIL":

                switch (payLoad[1]) {
                    case "ACK":
                        commandAndMessage = new String[]{payLoad[0] + " " + payLoad[1], payLoad[2],payLoad[3]};
                        break;

                    case "SND":
                        commandAndMessage = new String[]{payLoad[0] + " " + payLoad[1], payLoad[2], payLoad[3], payLoad[4]};
                        break;
                }

                break;

            case "ENC", "ENCSK", "ENCM":
                commandAndMessage = new String[]{command, payLoad[1], payLoad[2]};
                break;

            case "LST", "QUIT":
                commandAndMessage = new String[]{command};
                break;

            default:
                commandAndMessage = new String[]{command, message.split(" ", 2)[1]};
                break;
        }
        return commandAndMessage;
    }

    public void sendFile(byte[] fileBytes) throws IOException {
        OutputStream outputStream = fileSocket.getOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        // Sending file length and bytes
        dataOutputStream.writeInt(fileBytes.length);
        dataOutputStream.write(fileBytes);
    }

    public void writeToClient(String message) {
        writer.println(message);
        writer.flush();
    }

    public boolean checkIfLoggedIn() {
        if (status.equals(Statuses.LOGGED_IN) || status.equals(Statuses.AUTHENTICATED)) {
            return true;
        } else {
            writeToClient("ERR03 Please log in first");
            return false;
        }
    }

    public boolean checkName(String name) {
        Pattern pattern = Pattern.compile("[- !@#$%^&*()+=|/?.>,<`~]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(name);

        // Return true if there are no characters from the list above, and returns false, if the match is found
        return !matcher.find();
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

    public boolean isEncryptionSessionActive() {
        return encryptionSessionActive;
    }

    public void setEncryptionSessionActive(boolean encryptionSessionActive) {
        this.encryptionSessionActive = encryptionSessionActive;
    }
}
