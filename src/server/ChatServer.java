package server;

import clientHandler.ClientHandler;
import clientHandler.Group;
import clientHandler.Statuses;
import hashing.PasswordHash;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatServer {
    private ArrayList<ClientHandler> clientHandlers;
    private ArrayList<Group> groups;
    private PasswordHash passwordHash;

    public ChatServer() {
        clientHandlers = new ArrayList<>();
        this.groups = new ArrayList<>();
        this.passwordHash = new PasswordHash();
    }

    public void startServer() throws IOException {
        var serverSocket = new ServerSocket(1337);
        var serverFileSocket = new ServerSocket(1338);

        while (!serverSocket.isClosed()) {
            // Wait for an incoming client-connection request (blocking).
            Socket socket = serverSocket.accept();
            Socket fileSocket = serverFileSocket.accept();

            // Your code here:
            // TODO: Start a message processing and file thread for each connecting client.
            ClientHandler clientHandler = new ClientHandler(socket,this, fileSocket);
            clientHandler.start();

//            // TODO: Start a ping thread for each connecting client. NEEDS to be fixed
//            PingPongThread pongThread = new PingPongThread(socket.getOutputStream());
//            pongThread.start();
        }
    }

    /**
     * Login with username. If username already exists, but the status is disconnected, the old client handler is deleted from array list
     * while the new one is added with old information (username, password). There could be a better way of doing it, such as having Client
     * object separate from ClientHandler and instead of storing client handlers in array list, we would store clients objects. And thus,
     * we could delete client handler from client when it disconnects, and add new client handler when it connects. But we realised the
     * problem with disconnection too late, so we did not have time to implement this way properly.
     *
     * @param user User who wants to login with username
     * @param username Username user wants to have
     */
    public void loginUser(ClientHandler user, String username) {
        boolean usernameExists = false;
        ClientHandler foundUser = null;

        for (ClientHandler client: clientHandlers) {
            if (username.equalsIgnoreCase(client.getUsername())) {
                usernameExists = true;
                foundUser = client;
            }
        }

        if (!usernameExists) {
            user.setStatus(Statuses.LOGGED_IN);
            user.setUsername(username);
            clientHandlers.add(user);
            user.writeToClient("OK CONN " + username);
            System.out.println("OK CONN " + username);

        } else if(foundUser.getStatus() == Statuses.DISCONNECTED) {
            user.setStatus(Statuses.LOGGED_IN);
            user.setUsername(username);
            user.setPassword(foundUser.getPassword());
            clientHandlers.remove(foundUser);
            clientHandlers.add(user);
            user.writeToClient("OK CONN " + username);
            System.out.println("OK CONN " + username);
        } else {
            user.writeToClient("ERR01 This username already exists, choose another");
            System.out.println("ERR01 This username already exists, choose another");

        }

    }

    /**
     *  Password verification method
     * @param password password to check
     * @param clientHandler client who tries to authenticate
     * @throws IOException  when there is a failure during reading, writing, and searching file or directory operations.
     * @throws NoSuchAlgorithmException When the algorithm doesn't exist
     */
    public void authenticateMe(String password, ClientHandler clientHandler){
        try {
            if(clientHandler.getStatus() == Statuses.AUTHENTICATED){
                clientHandler.writeToClient("ERR13 Already authenticated");
                System.out.println("ERR13 Already authenticated");
            }else if(clientHandler.getPassword().equals("")) {
                clientHandler.writeToClient("ERR14 Create password");
                System.out.println("ERR14 Create password");

            } else {
                boolean result = passwordHash.checkPassword(password, clientHandler.getPassword());

                if (result) {
                    clientHandler.setStatus(Statuses.AUTHENTICATED);
                    clientHandler.writeToClient("OK AUTH");
                    System.out.println("OK AUTH");
                } else {
                    clientHandler.writeToClient("ERR11 Incorrect password");
                    System.out.println("ERR11 Incorrect password");
                }
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    /**
     *  create password method
     * @param password Password to hash
     * @param clientHandler Client who tries to create a password
     * @throws NoSuchAlgorithmException
     */
    public void createPassword(String password, ClientHandler clientHandler) throws InvalidKeySpecException, NoSuchAlgorithmException {

        Pattern pattern = Pattern.compile("[- !@#$%^&*()+=|/?.>,<`~]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(password);

        boolean matchFound = matcher.find();

        if(matchFound && (password.length() >= 8)){
            String hashedPasswordCreated = passwordHash.hashPassword(password);

            clientHandler.setPassword(hashedPasswordCreated);
            clientHandler.writeToClient("OK PASS");
            System.out.println("OK PASS");

        } else {
            clientHandler.writeToClient("ERR10 Weak Password");
            System.out.println("ERR10 Weak Password");
        }
    }

    /**
     * Send broadcast message to everyone connected to server
     * @param user User who wants to send the message
     * @param message Message text itself
     */
    public void sendBroadcastToEveryone(ClientHandler user, String message) {
        for (ClientHandler client: clientHandlers) {
            if(!client.equals(user)) {
                client.writeToClient("BCST " + user.getUsername() + " " + message);
                System.out.println("BCST " + user.getUsername() + " " + message);
            }
        }

        user.writeToClient("OK BCST " + message);
        System.out.println("OK BCST " + message);
    }

    /**
     * Send a list of all clients connected to the server
     * @param client Client who wants a list of clients
     */
    public void listAllClients(ClientHandler client) {
        String clientsList = "";

        for (int i = 0; i < clientHandlers.size(); i++) {
            ClientHandler loopingClient = clientHandlers.get(i);
            int authenticated = 0;
            if (loopingClient.checkIfAuthenticated()) {
                authenticated = 1;
            }

            clientsList += authenticated + " " + loopingClient.getUsername();

            if (i < clientHandlers.size() - 1) {
                clientsList += ",";
            }
        }

        client.writeToClient("OK LST " + clientsList);
        System.out.println("OK LST " + clientsList);
    }

    /**
     * Creating a new group
     * @param groupName name of the group that user wants to create
     */
    public void createGroup(String groupName, ClientHandler client){
        boolean exists = false;

        for (Group group: groups) {
            if (group.getGroupName().equals(groupName)) {
                exists = true;
                client.writeToClient("ERR05 Group with this name already exists");
                System.out.println("ERR05 Group with this name already exists");
                break;
            }
        }
        if (!exists) {
            groups.add(new Group(groupName));
            client.writeToClient("OK CRT");
            System.out.println("OK CRT");
        }
    }

    /**
     * List all groups present in the server
     * @param client who asked for list
     */
    public void listAllGroups(ClientHandler client){
        String response = "OK GRP LST ";

        for (Group group:groups) {
            response += group.getGroupName()+" , ";
        }
        client.writeToClient(response);
        System.out.println(response);
    }

    /**
     * Joining an existing group that was created
     * @param groupName name of the group to join
     * @param client name of the client who is joining
     */
    public void joinGroup(String groupName, ClientHandler client) {
        boolean exist = false;

        //If statement for the logged-in user line.
        for (Group group: groups) {
            if(group.getGroupName().equals(groupName)){

                if (!group.getClientsInGroup().contains(client)) {
                    boolean added = group.addToGroup(client);
                    if (added) {
                        client.writeToClient("OK JOIN " + groupName);
                        System.out.println("OK JOIN " + groupName);
                        System.out.println(client.getUsername() + " joined " + groupName + " group");
                    } else {
                        // todo: create error
                        System.out.println(client.getUsername() + " is already in the group!");
                    }
                } else {
                    client.writeToClient("ERR07 You are already in the group");
                    System.out.println("ERR07 You are already in the group");
                }
                exist = true;
            }
        }
        if (!exist) {
            client.writeToClient("ERR06 Group does not exist");
            System.out.println("ERR06 Group does not exist");
        }
    }

    /**
     * Leaving a group chat
     * @param groupName Group name client wants to leave
     * @param client Client who wants to leave
     */
    public void leaveGroupChat(String groupName, ClientHandler client){
        boolean exist = false;

        for (Group group:groups) {
            if(group.getGroupName().equals(groupName)){
                exist = true;
                boolean left = group.removeFromGroup(client);

                if (left) {
                    client.writeToClient("OK EXIT");
                    System.out.println("OK EXIT");
                    System.out.println(client.getUsername() + " left " + groupName + " group");
                } else {
                    client.writeToClient("ERR08 You are not a member of this group");
                    System.out.println("ERR08 You are not a member of this group");
                }
            }
        }
        if (!exist) {
            client.writeToClient("ERR06 Group does not exist!");
            System.out.println("ERR06 Group does not exist!");
        }
    }

    /**
     * Sending private message to a specific connected user
     * @param sender Sender of the message
     * @param receiverName Name of the receiver
     * @param message Private message text
     */
    public void sendPrivateMessage(ClientHandler sender, String receiverName, String message) {
        boolean exist = false;

        for (ClientHandler client: clientHandlers) {
            if (client.getUsername().equals(receiverName) && !receiverName.equals(sender.getUsername())) {
                exist = true;
                client.writeToClient("PMSG " + sender.getUsername() + " " + message);
                System.out.println("PMSG " + sender.getUsername() + " " + message);
                sender.writeToClient("OK PMSG");
                System.out.println("OK PMSG");
                break;
            }else if(receiverName.equals(sender.getUsername())){
                sender.writeToClient("ERR18 You cannot send anything to yourself");
                System.out.println("ERR18 You cannot send anything to yourself");
            }
        }
        if (!exist) {
            sender.writeToClient("ERR09 Username does not exist");
            System.out.println("ERR09 Username does not exist");
        }
    }

    /**
     * Method to send an acknowledgement to a client
     * @param sender Name of client sender
     * @param receiverName name of client receiver
     * @param filePath is the absolute file path of te doc to be sent
     */
    public void sendAcknowledgement(ClientHandler sender, String receiverName, String filePath){
        ClientHandler foundClient = null;

        for (ClientHandler client: clientHandlers) {
            if (client.getUsername().equals(receiverName)) {
                foundClient = client;
            }
        }
        if (foundClient != null && !receiverName.equals(sender.getUsername())) {
            foundClient.writeToClient("ACK "+ sender.getUsername() + " "+ filePath);
            System.out.println("ACK "+ foundClient.getUsername() + " "+ filePath);
        } else if (foundClient == null) {
            sender.writeToClient("ERR09 Username does not exist");
            System.out.println("ERR09 Username does not exist");
        } else if (receiverName.equals(sender.getUsername())) {
            sender.writeToClient("ERR18 You cannot send anything to yourself");
            System.out.println("ERR18 You cannot send anything to yourself");
        }
    }

    /**
     * Method to respond to an acknowledgement
     * @param sender name of the sender client
     * @param receiverName name of the receiver client
     * @param response the message input by the client
     */
    public void respondToAck(ClientHandler sender, String receiverName, String response, String filePath){

        boolean result = false;

        for (ClientHandler client: clientHandlers) {
            if (client.getUsername().equals(receiverName)) {
                result = true;
                switch (response) {
                    case "ACC":
                        client.writeToClient("FIL ACC "+ sender.getUsername()+ " "+ filePath);
                        System.out.println("FIL ACC "+ sender.getUsername()+ " "+ filePath);
                        break;
                    case "DEC":
                        client.writeToClient("FIL DEC " + sender.getUsername() +" "+filePath);
                        System.out.println("FIL DEC " + sender.getUsername() +" "+filePath);
                        break;
                }
            }
        }
        if (!result) {
            sender.writeToClient("ERR09 Username does not exist");
            System.out.println("ERR09 Username does not exist");
        }
    }

    /**
     * Send broadcast message to the group
     * @param sender Sender of the message
     * @param groupName Group name sender wants to send message to
     * @param message Message text
     */
    public void sendBroadcastToGroup(ClientHandler sender, String groupName, String message) {
        boolean exist = false;

        for (Group group: groups) {
            if (group.getGroupName().equals(groupName)) {
                exist = true;

                if (group.getClientsInGroup().contains(sender)) {
                    for (ClientHandler clientHandler : group.getClientsInGroup()) {
                        clientHandler.writeToClient("GRP BCST " + groupName + " " + sender.getUsername() + " " + message);
                        sender.writeToClient("OK GRP BCST");
                        System.out.println("GRP BCST " + groupName + " " + sender.getUsername() + " " + message);
                        System.out.println("OK GRP BCST");
                    }
                } else
                    sender.writeToClient("ERR08 You are not a member of this group");
                    System.out.println("ERR08 You are not a member of this group");
            }
        }
        if (!exist) {
            sender.writeToClient("ERR06 Group does not exist");
            System.out.println("ERR06 Group does not exist");
        }
    }

    /**
     * Send file to client
     * @param sender Sender of the file
     * @param receiver Name of the client who will receive file
     * @param checkSum Checksum of the file
     * @param filename Filename
     */
    public void sendFileToClient(ClientHandler sender, String receiver, String checkSum, String filename){
        boolean exist = false;

        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.getUsername().equals(receiver)) {
                clientHandler.writeToClient("INC " + sender.getUsername() + " " + checkSum + " " + filename);
                System.out.println("INC " + sender.getUsername() + " " + checkSum + " " + filename);
                exist = true;
            }
        }
        if(!exist){
          sender.writeToClient("ERR09 Username does not exist");
          System.out.println("ERR09 Username does not exist");
        }
    }

    /**
     * Method that returns the client
     * @param username of the client we need
     * @return the client
     */
    public ClientHandler getClientByName(String username) {

        for (ClientHandler client: clientHandlers) {
            if (client.getUsername().equals(username)) {
                return client;
            }
        }
        return null;
    }

    //The following methods only deal with encryption
    /**
     * First method in server, passes ENC to receiving client
     * @param sender name of the sender
     * @param receiverName name of the receiver
     * @param publicKey the senders public key
     */
    public void forwardClientsPublicKey(ClientHandler sender, String receiverName, String publicKey){
        boolean exist = false;

        for (ClientHandler client: clientHandlers) {
            if (client.getUsername().equals(receiverName) && !sender.getUsername().equals(receiverName)) {
                exist = true;
                client.writeToClient("ENC " + sender.getUsername() + " " + publicKey);
                System.out.println("ENC " + sender.getUsername() + " " + publicKey);
                break;
            }
        }

        if (!exist && !sender.getUsername().equals(receiverName)) {
            sender.writeToClient("ERR09 Username does not exist");
            System.out.println("ERR09 Username does not exist");
        } else if (sender.getUsername().equals(receiverName)){
            sender.writeToClient("ERR18 You cannot send anything to yourself");
            System.out.println("ERR18 You cannot send anything to yourself");

        }
    }

    /**
     * Method to pass on the session key to the other client
     * @param sender who sent the session key
     * @param receiverName who will receive the session key
     * @param encryptedSessionKey the session key that had been encrypted
     */
    public void forwardEncryptedSessionKey(ClientHandler sender, String receiverName, String encryptedSessionKey){
        for (ClientHandler client: clientHandlers) {
            if (client.getUsername().equals(receiverName)) {

                client.writeToClient("ENCSK " + sender.getUsername() + " " + encryptedSessionKey);
                System.out.println("ENCSK " + sender.getUsername() + " " + encryptedSessionKey);
                break;
            }
        }
    }

    /**
     * Method to pass on the encrypted message t the receiving client
     * @param sender who sent the message
     * @param receiverName who will receive the message
     * @param encryptedMessage the message encrypted by the sender
     */
    public void forwardEncryptedMessageToclient(ClientHandler sender, String receiverName, String encryptedMessage){
        for (ClientHandler client: clientHandlers) {
            if (client.getUsername().equals(receiverName)) {

                client.writeToClient("ENCM " + sender.getUsername() + " " + encryptedMessage);
                System.out.println("ENCM " + sender.getUsername() + " " + encryptedMessage);
                break;
            }
        }
    }

    /**
     * Method that disconnects the user from the user
     * @param client who wants to disconnect
     */
    public void disconnectFromTheServer(ClientHandler client) {
        if (clientHandlers.contains(client)) {
            client.setStatus(Statuses.DISCONNECTED);
            System.out.println(client.getStatus());
            client.writeToClient("OK QUIT");
            System.out.println("OK QUIT");
        }
    }
}
