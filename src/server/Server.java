package server;

import client.ClientHandler;
import client.Group;
import client.Statuses;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
    private ArrayList<ClientHandler> clientHandlers;
    private ArrayList<Group> groups;
    private String[] commands;

    public Server() {
        clientHandlers = new ArrayList<>();
        this.groups = new ArrayList<>();
        commands = new String[]{"CONN", "BCST", "QUIT", "AUTH", "LST", "GRP CRT", "GRP LST", "GRP EXIT", "GRP JOIN",
                "GRP BCST", "PMSG","FIL ACK","FIL SND","INC"};
    }

    public void startServer() throws IOException {
        var serverSocket = new ServerSocket(1337);
        var serverFileSocket = new ServerSocket(1338);

        while (true) {
            // Wait for an incoming client-connection request (blocking).
            Socket socket = serverSocket.accept();

            // Your code here:
            // TODO: Start a message processing and file thread for each connecting client.
            ClientHandler clientHandler = new ClientHandler(socket,this);
            clientHandler.start();

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
     * @param password
     * @param clientHandler
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * Password verification method
     */
    public void authenticateMe(String password,ClientHandler clientHandler){

        for (ClientHandler client:clientHandlers) {
            if(client.getUsername().equals(clientHandler.getUsername())){
                if(client.getStatus().equals("AUTHENTICATED")){
                    client.writeToClient("ERR11 Already authenticated");
                }else{
                    if(client.getPassword().equals("")){
                        client.writeToClient("ERR 12 Create password");
                    }else if(BCrypt.checkpw(password,client.getPassword())){
                        client.setStatus(Statuses.AUTHENTICATED);
                        client.writeToClient("OK AUTH");
                        //TODO: Implement authentication feature for noticing authed usernames
                    }
                }
            }else{
                clientHandler.writeToClient("ERR12 This is not your username");
            }
        }
    }

    /**
     *
     * @param passWord
     * @param clientHandler
     * @throws NoSuchAlgorithmException
     * create password method
     */
    public void createPassword(String passWord,ClientHandler clientHandler) throws NoSuchAlgorithmException {
        for (ClientHandler client:clientHandlers) {
            if((client.getUsername().equals(clientHandler.getUsername()))&&(!client.getPassword().equals(passWord))) {
                if((passWord.length() >= 8)){
                    Pattern pattern = Pattern.compile("[- !@#$%^&*()+=|/?.>,<`~]", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(passWord);
                    boolean matchFound = matcher.find();

                    if(matchFound){
                        SecureRandom random = new SecureRandom();
                        byte[] salt = new byte[16];
                        random.nextBytes(salt);

                        MessageDigest md = MessageDigest.getInstance("SHA-512");
                        md.update(salt);

                        byte[] hashedPassword = md.digest(passWord.getBytes(StandardCharsets.UTF_8));

                        String hashedPasswordCreated = new String(hashedPassword, StandardCharsets.UTF_8);

                        client.writeToClient("OK PASS");
                        client.setPassword(hashedPasswordCreated);
                        System.out.println("password created");
                    }else{
                        client.writeToClient("ERR08 Weak Password");
                        System.out.println("Weak password");
                    }
                    break;
                }else{
                    client.writeToClient("ERR10 Invalid password");
                    System.out.println("Invalid password");
                }
            }
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

            clientsList += authenticated + " " + loopingClient;

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
                    group.addToGroup(client);
                    client.writeToClient("OK JOIN " + groupName);
                    System.out.println("OK JOIN");
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
                group.removeFromGroup(client);
                client.writeToClient("OK EXIT");
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
            }
        }
        if (!exist) {
            sender.writeToClient("ERR... Client does not exist!");
            System.out.println("ERR... Client does not exist!");
        }
    }

    /**
     * Method to send an acknowledgement to a client
     * @param sender Name of client sender
     * @param receiverName name of client receiver
     */
    public void sendAcknowledgement(ClientHandler sender, String receiverName){
        boolean exist = false;

        for (ClientHandler client: clientHandlers) {
            if (client.getUsername().equals(receiverName)) {
                exist = true;
                client.writeToClient("ACK "+ sender.getUsername());
                System.out.println("ACK forwarded to client "+client.getUsername());
            }
        }
        if (!exist) {
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
    public void respondToAck(ClientHandler sender,String receiverName,String response){

        boolean result = false;

        for (ClientHandler client: clientHandlers) {
            if (client.getUsername().equals(receiverName)) {
                result = true;
                switch (response) {
                    case "ACC":
                        client.writeToClient("FIL ACC "+ sender.getUsername());
                        client.writeToClient("INFO: Ready for file transmission");
                        break;
                    case "DEC":
                        client.writeToClient("FIL DEC " + sender.getUsername());
                        client.writeToClient("INFO: File transmission cannot be done");
                }
            }
        }

        if (!result) {
            sender.writeToClient("ERR07 Username doesn't exist");
        }


    }

    public void sendBroadcastToGroup(ClientHandler sender, String groupName, String message) {
        boolean exist = false;

        for (Group group: groups) {
            if (group.getGroupName().equals(groupName)) {
                exist = true;

                if (group.getClientsInGroup().contains(sender)) {
                    for (ClientHandler clientHandler: group.getClientsInGroup()) {
                        clientHandler.writeToClient("GRP BCST " + groupName + " " + sender.getUsername() + " " + message);
                    }
                }
            }
        }
        if (!exist) {
            sender.writeToClient("ERR... client.client.Group does not exist!");
        }
    }

    public ArrayList<ClientHandler> getClients() {
        return clientHandlers;
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
