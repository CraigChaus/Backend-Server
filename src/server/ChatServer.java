package server;

import clientHandler.ClientHandler;
import clientHandler.Group;
import clientHandler.Statuses;
import fileHandler.ClientFileHandler;
import fileHandler.FileServer;
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
    private String[] commands;
    private PasswordHash passwordHash;
    private FileServer fileServer;
    private long primeKeyG,rootKeyG;

    public ChatServer() {
        clientHandlers = new ArrayList<>();
        this.groups = new ArrayList<>();
        commands = new String[]{"CONN", "BCST", "QUIT", "AUTH", "LST", "GRP CRT", "GRP LST", "GRP EXIT", "GRP JOIN",
                "GRP BCST", "PMSG","FIL ACK","FIL SND","INC"};
        this.passwordHash = new PasswordHash();
        fileServer = new FileServer();

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

            ClientFileHandler clientFileHandler = new ClientFileHandler(clientHandler, fileSocket);
            clientFileHandler.start();


            // TODO: Start a ping thread for each connecting client.
            PingPongThread pongThread = new PingPongThread(socket.getOutputStream());
            pongThread.start();
        }
    }

    public void loginUser(ClientHandler user, String username) {
        boolean usernameExists = false;

        for (ClientHandler client: clientHandlers) {
            if (username.equalsIgnoreCase(client.getUsername())) {
                usernameExists = true;
            }
        }

        if (!usernameExists) {
            user.setStatus(Statuses.LOGGED_IN);
            user.setUsername(username);
            clientHandlers.add(user);
            user.writeToClient("OK CONN " + username);
            System.out.println("OK CONN " + username);

        } else {
            user.writeToClient("ERR01 This username already exists");
        }

    }

    /**
     *
     * @param password password to check
     * @param clientHandler client who tries to authenticate
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * Password verification method
     */
    public void authenticateMe(String password, ClientHandler clientHandler){

        try {
            if(clientHandler.getStatus() == Statuses.AUTHENTICATED){
                clientHandler.writeToClient("ERR11 Already authenticated");
            }else if(clientHandler.getPassword().equals("")) {

                clientHandler.writeToClient("ERR 12 Create password");

            } else {
                System.out.println("Clients password to check: " + clientHandler.getPassword());
                boolean result = passwordHash.checkPassword(password, clientHandler.getPassword());
                System.out.println("IS AUTHENTICATED: " + result);

                if (result) {
                    clientHandler.setStatus(Statuses.AUTHENTICATED);
                    clientHandler.writeToClient("OK AUTH");
                    //TODO: Implement authentication feature for noticing authed usernames
                }

            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param password
     * @param clientHandler
     * @throws NoSuchAlgorithmException
     * create password method
     */
    public void createPassword(String password, ClientHandler clientHandler) throws InvalidKeySpecException, NoSuchAlgorithmException {

        if((password.length() >= 8)) {
            Pattern pattern = Pattern.compile("[- !@#$%^&*()+=|/?.>,<`~]", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(password);
            boolean matchFound = matcher.find();

            if(matchFound){

//                String salt = BCrypt.gensalt();
//                String hashedPasswordCreated = BCrypt.hashpw(passWord, salt);

                String hashedPasswordCreated = passwordHash.hashPassword(password);
                System.out.println(hashedPasswordCreated);

                clientHandler.setPassword(hashedPasswordCreated);
                clientHandler.writeToClient("OK PASS");
                System.out.println("password created");

            }else{
                clientHandler.writeToClient("ERR08 Weak Password");
                System.out.println("Weak password");
            }

        }else{
            clientHandler.writeToClient("ERR10 Invalid password");
            System.out.println("Invalid password");
        }
    }

    public void sendBroadcastToEveryone(ClientHandler user, String message) {
        for (ClientHandler client: clientHandlers) {
            client.writeToClient(commands[1] + " " + user.getUsername() + " " + message);
        }

        user.writeToClient("OK " + commands[1] + " " + message);
    }

    /**
     * METHOD 1: Printing out a list of all clients connected to the server
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

        client.writeToClient("OK " + commands[4] + " " + clientsList);
        System.out.println("OK " + commands[4] + " " + clientsList);
    }

    /**
     * METHOD2: Creating a new group
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
     * METHOD 3: Listing all the groups connected to the server
     */
    public void listAllGroups(ClientHandler client){

        String response = "OK GRPLST ";

        for (Group group:groups) {
            response += group.getGroupName()+", ";
        }

        client.writeToClient(response);
        System.out.println("OK GRPLST");
    }

    //THIS METHOD IS MEANT TO LIST ALL CLIENTS IN A GROUP
//    public void listAllClientsInGroup(client.client.client.Group groupName){
//
//        PrintWriter writer = new PrintWriter(outputStream);
//
//        if(groups.contains(groupName)){
//
//            writer.println("OK LST ");
//            writer.flush();
//
//            for (client.client.client.Group group:groups) {
//                writer.print(group.getClientsInGroup()+" , ");
//                writer.flush();
//            }
//
//        }
//    }

    /**
     * Method 4: Joining an existing group
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
            client.writeToClient("ER06 Group does not exist");
            System.out.println("ER06 Group does not exist");
        }
    }

    /**
     * Method 5: Leaving a group chat
     * @param groupName
     * @param client
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
            client.writeToClient("ERR06 client.client.Group does not exist!");
        }
    }

    /**
     * Sending private message
     * @param sender
     * @param receiverName
     * @param message
     */
    public void sendPrivateMessage(ClientHandler sender, String receiverName, String message) {
        boolean exist = false;

        for (ClientHandler client: clientHandlers) {
            if (client.getUsername().equals(receiverName)) {
                exist = true;
                client.writeToClient("PMSG " + sender.getUsername() + " " + message);
                sender.writeToClient("OK PMSG");
                System.out.println("OK PMSG");
                break;
            }
        }
        if (!exist) {
            sender.writeToClient("ERR07 Username does not exist!");
            System.out.println("ERR07 Username does not exist!");
        }
    }

    /**
     * Method to send an acknowledgement to a client
     * @param sender Name of client sender
     * @param receiverName name of client receiver
     * @param filePath is the absolute file path of te doc to be sent
     */
    public void sendAcknowledgement(ClientHandler sender, String receiverName, String filePath){
        boolean exist = false;

        for (ClientHandler client: clientHandlers) {
            if (client.getUsername().equals(receiverName)) {
                exist = true;
                client.writeToClient("ACK "+ sender.getUsername() + " "+ filePath);
                System.out.println("ACK forwarded to client "+client.getUsername()+ " file: "+filePath);
            }
        }
        if (!exist) {
            // todo: check this part
            sender.writeToClient("ERR03 Please log in first");
            System.out.println("ERR07 Username does not exist");
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
            // todo: check this part
            sender.writeToClient("ERR... client.client.Group does not exist!");
        }
    }

//    public void sendFileToClient(ClientHandler sender, String receiver, String filePath, String checkSum){
//        boolean exist = false;
//
//        for (ClientHandler clientHandler : clientHandlers) {
//            if (clientHandler.getUsername().equals(receiver)) {
//
//                clientHandler.writeToClient("INC " + sender.getUsername() + " " + checkSum + " " + filePath);
//                fileServer.sendToClient(sender, clientHandler,filePath);
//                exist = true;
//                System.out.println("sent file from chatserver");
//            }
//        }
//        if(!exist){
//          sender.writeToClient("ERR07 Username doesn't exist");
//        }
//    }

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
        if (client.getUsername().equals(receiverName)) {
            exist = true;
            client.writeToClient("ENC " + sender.getUsername() + " " + publicKey);
            System.out.println("Sent ENC and public key to :"+ receiverName+ " public key "+ publicKey);
            break;
        }
    }
        if (!exist) {
        sender.writeToClient("ERR07 Username does not exist");
        System.out.println("ERR07 Username does not exist");
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
