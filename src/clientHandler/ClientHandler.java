package clientHandler;

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

                if (status != Statuses.CONNECTED) {
                    writeToClient("ERR you are already logged in!");
                    break;
                }
                String username = command[1];

                boolean isUsernameAcceptable = checkUsername(username);

                if (!isUsernameAcceptable||command.length < 2) {
                    writeToClient("ERR02 Username has an invalid format or is empty(only characters, numbers and underscores are allowed)");
                    //TODO: change on protocol
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
//                    chatServer.sendFileToClient(this,command[1],command[3], command[2]);

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

            default:
                writeToClient("ERR00 Unknown command");
        }
    }

    public String[] parseMessage(String message) {
        String command = message.split(" ")[0];
        //The first element of the array is command, the second is message
        String[] commandAndMessage = new String[]{};
        String[] payLoad = message.split(" ");

        //TODO: Handle all the commands like you did here
        switch (command) {
            case "GRP" -> {

                switch (payLoad[1]) {
                    case "CRT", "JOIN", "EXIT" -> commandAndMessage = new String[]{payLoad[0] + " " + payLoad[1], payLoad[2]};

                    case "BCST" -> {
                        String[] splitMessage = message.split(" ", 4);
                        commandAndMessage = new String[]{splitMessage[0] + " " + splitMessage[1], splitMessage[2], splitMessage[3]};
                    }

                    case "LST" -> commandAndMessage = new String[]{payLoad[0] + " " + payLoad[1]};
                }

           }
            case "PMSG"-> {
                String[] splitMessage = message.split(" ", 3);
                commandAndMessage = new String[]{splitMessage[0], splitMessage[1], splitMessage[2]};
            }

            case "FIL" -> {
                if (message.split(" ")[1].equals("ACK")) {
                    String[] splitMessageAck = message.split(" ");
                    commandAndMessage = new String[]{splitMessageAck[0] + " " + splitMessageAck[1], splitMessageAck[2],splitMessageAck[3]};
                } else if (message.split(" ")[1].equals("SND")) {
                    String[] splitMessageSnd = message.split(" ");
                    commandAndMessage = new String[]{splitMessageSnd[0] + " " + splitMessageSnd[1], splitMessageSnd[2], splitMessageSnd[3], splitMessageSnd[4]};
                }
            }

            case "ENC", "ENCSK" -> {
                commandAndMessage = new String[]{command, payLoad[1], payLoad[2]};
            }
            default -> commandAndMessage = new String[]{command, message.split(" ", 2)[1]};
        }
        return commandAndMessage;
    }

    public void transferFile(String receiver, byte[] fileBytes) {

        ClientHandler receiverClient = chatServer.getClientByName(receiver);

        if (receiverClient == null) {
            System.out.println("Client to send file to is not found!");
        } else {
            try {
                System.out.println("We are transferring file to " + receiverClient.getUsername());
                receiverClient.sendFile(fileBytes);
            } catch (IOException e) {
                System.err.println("Exception in transferring file!");

            }

        }
    }

    public void sendFile(byte[] bytes) throws IOException {
        System.out.println("Send file method of client: " + username);
        OutputStream outputStream = fileSocket.getOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        dataOutputStream.writeInt(bytes.length);
        dataOutputStream.write(bytes);
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

    public boolean checkUsername(String username) {
        Pattern pattern = Pattern.compile("[- !@#$%^&*()+=|/?.>,<`~]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(username);

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
