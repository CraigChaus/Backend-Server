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
    private long primeKeyG,rootKeyG;

    public ChatServer() {
        clientHandlers = new ArrayList<>();
        this.groups = new ArrayList<>();
        this.passwordHash = new PasswordHash();

        //TODO: change the values later on when testing
        primeKeyG = 23;
        rootKeyG = 9;
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

            // TODO: Start a ping thread for each connecting client.
            PingPongThread pongThread = new PingPongThread(socket.getOutputStream());
            pongThread.start();
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
            user.writeToClient("ERR01 This username already exists");
        }

    }

    /**
     *  Password verification method
     * @param password password to check
     * @param clientHandler client who tries to authenticate
     * @throws IOException
     * @throws NoSuchAlgorithmException
     *
     */
    public void authenticateMe(String password, ClientHandler clientHandler){

        try {
            if(clientHandler.getStatus() == Statuses.AUTHENTICATED){
                clientHandler.writeToClient("ERR11 Already authenticated");
            }else if(clientHandler.getPassword().equals("")) {

                clientHandler.writeToClient("ERR 12 Create password");

            } else {
                boolean result = passwordHash.checkPassword(password, clientHandler.getPassword());

                if (result) {
                    clientHandler.setStatus(Statuses.AUTHENTICATED);
                    clientHandler.writeToClient("OK AUTH");
                } else {
                    clientHandler.writeToClient("ERR10 Invalid password");
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
     *
     */
    public void createPassword(String password, ClientHandler clientHandler) throws InvalidKeySpecException, NoSuchAlgorithmException {

        Pattern pattern = Pattern.compile("[- !@#$%^&*()+=|/?.>,<`~]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(password);

        boolean matchFound = matcher.find();

        if(matchFound && (password.length() >= 8)){
            String hashedPasswordCreated = passwordHash.hashPassword(password);

            clientHandler.setPassword(hashedPasswordCreated);
            clientHandler.writeToClient("OK PASS");

        } else {
            clientHandler.writeToClient("ERR08 Weak Password");
        }


    }

    /**
     * Send broadcast message to everyone
     * @param user User who wants to send the message
     * @param message Message text
     */
    public void sendBroadcastToEveryone(ClientHandler user, String message) {
        for (ClientHandler client: clientHandlers) {
            client.writeToClient("BCST " + user.getUsername() + " " + message);
        }

        user.writeToClient("OK " + "BCST " + message);
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

        Pattern pattern = Pattern.compile("[- !@#$%^&*()+=|/?.>,<`~]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(groupName);
        boolean matchFound = matcher.find();

        if(matchFound){

            client.writeToClient("Invalid Group name, please use letters and numbers only :) e.g MangoJuju6969");

        }else{
            boolean exists = false;

            for (Group group: groups) {
                if (group.getGroupName().equals(groupName)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                groups.add(new Group(groupName));
                client.writeToClient("OK CRT");
                System.out.println("OK CRT " + client.getUsername());
            }
        }
    }

    /**
     * List all groups
     * @param client Client who asked for list
     */
    public void listAllGroups(ClientHandler client){

        String response = "OK GRPLST ";

        for (Group group:groups) {
            response += group.getGroupName()+", ";
        }

        client.writeToClient(response);
        System.out.println("OK GRPLST");
    }

    /**
     * Joining an existing group
     * @param groupName
     * @param client
     */
    public void joinGroup(String groupName, ClientHandler client) {
        boolean exist = false;

        //IF statement for the logged in user line.
        for (Group group: groups) {
            if(group.getGroupName().equals(groupName)){

                if (!group.getClientsInGroup().contains(client)) {
                    boolean added = group.addToGroup(client);
                    if (added) {
                        client.writeToClient("OK JOIN " + groupName);
                        System.out.println(client.getUsername() + " joined " + groupName + " group");
                    } else {
                        // todo: create error
                        System.out.println(client.getUsername() + " is already in the group!");
                    }

                } else {
                    client.writeToClient("ERR18 You are already in this group!");
                    System.out.println("ERR18 You are already in this group!");
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
                    System.out.println(client.getUsername() + " left " + groupName + " group");
                } else {
                    // todo: create error for client side
                    System.out.println(client.getUsername() + " is not in " + groupName + " group");
                }

            }
        }

        if (!exist) {
            client.writeToClient("ERR06 Group does not exist!");
        }
    }

    /**
     * Sending private message
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
                sender.writeToClient("OK PMSG");
                System.out.println("OK PMSG");
                break;
            }
        }
        if (!exist) {
            sender.writeToClient("ERR16 You cannot send anything to yourself");
            System.out.println("ERR07 Username does not exist");
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
            System.out.println("ACK forwarded to client " + foundClient.getUsername() + " file: " + filePath);
        } else if (foundClient == null) {
            sender.writeToClient("ERR07 Username does not exist");
            System.out.println("ERR07 Username does not exist");
        } else if (receiverName.equals(sender.getUsername())) {
            sender.writeToClient("ERR16 You cannot send anything to yourself");
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
                        System.out.println("Sent FIL ACC to "+ sender.getUsername() +  " "+ filePath);
                        System.out.println("INFO: Ready for file transmission");
                        break;
                    case "DEC":
                        client.writeToClient("FIL DEC " + sender.getUsername() +" "+filePath);
                        System.out.println("Sent FIL DEC to "+ sender.getUsername() +  " "+ filePath);
                        System.out.println("INFO: File transmission cannot be done");
                        break;
                }
            }
        }
        if (!result) {
            sender.writeToClient("ERR07 Username doesn't exist");
            System.out.println("ERR07 Username doesn't exist");
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
                        System.out.println("GRP BCST " + groupName + " " + sender.getUsername() + " " + message);
                    }
                } else
                    System.out.println("Sender is not in the group!");
            }
        }
        if (!exist) {
            sender.writeToClient("ERR06 Group does not exist");
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
          sender.writeToClient("ERR07 Username doesn't exist");
        }
    }

    public ArrayList<ClientHandler> getClients() {
        return clientHandlers;
    }


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
                System.out.println("Sent ENC and public key to :"+ receiverName+ " public key "+ publicKey);
                break;
            }
        }

        if (!exist && !sender.getUsername().equals(receiverName)) {
            sender.writeToClient("ERR07 Username does not exist");
            System.out.println("ERR07 Username does not exist");
        } else if (sender.getUsername().equals(receiverName)){
            sender.writeToClient("ERR16 You cannot send anything to yourself");
        }
    }

    public void forwardEncryptedSessionKey(ClientHandler sender, String receiverName, String encryptedSessionKey){
        for (ClientHandler client: clientHandlers) {
            if (client.getUsername().equals(receiverName)) {

                client.writeToClient("ENCSK " + sender.getUsername() + " " + encryptedSessionKey);

                //TODO: For testing purposes only, please delete when it works perfectly
                System.out.println("Sent ENCSK and session key to :"+ receiverName+ " session key "+ encryptedSessionKey);
                break;
            }
        }
    }

    public void forwardEncryptedMessageToclient(ClientHandler sender, String receiverName, String encryptedMessage){
        for (ClientHandler client: clientHandlers) {
            if (client.getUsername().equals(receiverName)) {

                client.writeToClient("ENCM " + sender.getUsername() + " " + encryptedMessage);

                //TODO: For testing purposes only, please delete when it works perfectly
                System.out.println("Sent ENCM and message to :"+ receiverName+ " ++Message: "+ encryptedMessage);
                break;
            }
        }
    }

    public void disconnectFromTheServer(ClientHandler client) {
        if (clientHandlers.contains(client)) {
            client.setStatus(Statuses.DISCONNECTED);
            System.out.println(client.getStatus());
            client.writeToClient("OK QUIT");
            System.out.println("OK QUIT");
        }
    }

    public void setClients(ArrayList<ClientHandler> clientHandlers) {
        this.clientHandlers = clientHandlers;
    }

    public ArrayList<Group> getGroups() {
        return groups;
    }

    public void setGroups(ArrayList<Group> groups) {
        this.groups = groups;
    }



}
