package clientHandler;

import fileHandler.ClientFileHandler;
import server.ChatServer;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
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
                    System.out.println("<<<< PONG");
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

    /**
     * Method looks at the command and executes a method for that command. The message would have been separated
     * strategically by the parseMessage method.
     * @param message the input from the client to be seperated by parseMessage
     * @throws IOException when there is a failure during reading, writing, and searching file or directory operations.
     */
    public void processMessage(String message) throws IOException {
        String[] command = parseMessage(message);

        //Switch statement based on the command sent with the message
        switch (command[0]) {
            case "CONN":
                if (status == Statuses.CONNECTED || status == Statuses.DISCONNECTED) {
                    String username = command[1];

                    boolean isUsernameAcceptable = checkName(username);

                    if (!isUsernameAcceptable || username == null) {
                        writeToClient("ERR02 Name has an invalid format or is empty(only characters, numbers and underscores are allowed)");
                        System.out.println("ERR02 Name has an invalid format or is empty(only characters, numbers and underscores are allowed)");

                    } else {
                        chatServer.loginUser(this, username); //connect the user
                    }
                } else {
                    writeToClient("ERR04 You are already logged in");
                    System.out.println("ERR04 You are already logged in");
                }

                break;
            case "BCST":
                if (checkIfLoggedIn()) {
                    String userMessage = command[1];

                    chatServer.sendBroadcastToEveryone(this, userMessage); //Send a broadcast
                }
                break;
            case "LST":
                if (checkIfLoggedIn()) {
                    chatServer.listAllClients(this);// List all clients connected to server, authenticated and not
                }
                break;

            case "GRP CRT":
                if (checkIfLoggedIn()) {
                    if (checkName(command[1]))
                        chatServer.createGroup(command[1], this);
                }else{
                    writeToClient("ERR02 Name has an invalid format (only characters, numbers and underscores are allowed)");
                    System.out.println("ERR02 Name has an invalid format (only characters, numbers and underscores are allowed)");
                }
                break;

            case "GRP JOIN":
                if (checkIfLoggedIn()) {
                    chatServer.joinGroup(command[1], this); //Join the group
                }
                break;

            case "GRP LST":
                if (checkIfLoggedIn()) {
                    chatServer.listAllGroups(this); // List all the active groups
                }
                break;

            case "GRP BCST":
                if (checkIfLoggedIn()) {
                    chatServer.sendBroadcastToGroup(this, command[1], command[2]); //Send a broadcast message
                }
                break;

            case "GRP EXIT":
                if (checkIfLoggedIn()) {
                    chatServer.leaveGroupChat(command[1], this); // Leave the group
                }
                break;

            case "PMSG":
                if (checkIfLoggedIn()) {
                    chatServer.sendPrivateMessage(this, command[1], command[2]); //Send private message
                }
                break;

            case "FIL ACK":
                if(checkIfLoggedIn()){
                    chatServer.sendAcknowledgement(this, command[1],command[2]); //Send Acknowledgement for file transfer

                }
                break;

            case "FIL SND":
                // Send file if logged id
                if(checkIfLoggedIn()){
                    chatServer.sendFileToClient(this, command[1], command[2], command[3]); //Send the file
                    ClientHandler receiverClient = chatServer.getClientByName(command[1]);
                    new ClientFileHandler(this, receiverClient, fileSocket).start();
                }
                break;

            case "ACC":
                chatServer.respondToAck(this, message.split(" ")[1], "ACC",message.split(" ")[2]); //Accept the Acknowledgement received
                break;

            case "DEC":
                chatServer.respondToAck(this, command[1], "DEC",command[2]);//Decline the Acknowledgement received
                break;

            case "PASS":
                try {
                    chatServer.createPassword(message.split(" ")[1], this); //Create the password to authenticate the user
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    e.printStackTrace();
                }
                break;

            case "AUTH":
                chatServer.authenticateMe(message.split(" ")[1], this); //Authenticate user
                break;

            case "ENC":
                chatServer.forwardClientsPublicKey(this,command[1],command[2]); //Pass on the public key to the other client
                break;

            case "ENCSK":
                chatServer.forwardEncryptedSessionKey(this,command[1],command[2]);//Pass on the session key to other client
                break;

            case "ENCM":
                chatServer.forwardEncryptedMessageToclient(this,command[1],command[2]); //Pass on the encrypted message to the other client
                break;

            case "QUIT":
                chatServer.disconnectFromTheServer(this);// Disconnect from the server
                messageSocket.close();
                System.out.println("User " + username + " disconnected");
                break;

            default:
                writeToClient("ERR00 Unknown command");
                System.out.println("ERR00 Unknown command");
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

            case "QUIT":
                commandAndMessage = new String[]{command};
                break;

            default:
                commandAndMessage = new String[]{command,payLoad[1]};

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

    /**
     * Special method that writes/sends the message to the respective client
     * @param message that will be sent
     */
    public void writeToClient(String message) {
        writer.println(message);
        writer.flush();
    }

    /**
     * Method to check if user is logged in before the other commands (not CONN)
     * can be executed
     * @return boolean whether they are connected or not
     */
    public boolean checkIfLoggedIn() {
        if (status.equals(Statuses.LOGGED_IN) || status.equals(Statuses.AUTHENTICATED)) {
            return true;
        } else {
            writeToClient("ERR03 Please log in first");
            System.out.println("ERR03 Please log in first");
            return false;
        }
    }

    /**
     * checks if the input is of agreed/valid format
     * @param name of the user or group
     * @return whether names acceptable or not
     */
    public boolean checkName(String name) {

        Pattern pattern = Pattern.compile("[- !@#$%^&*()+=|/?.>,<`~]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(name);

        // Return true if there are no characters from the list above, and returns false, if the match is found
        return !matcher.find();
    }

    /**
     * checks if the user is authenticated
     * @return whether user is authenticated or not
     */
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
}
