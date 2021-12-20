import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.*;
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
    private final String[] commands;

    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader serverReader;
    private PrintWriter writer;

    public Server() {
        clientHandlers = new ArrayList<>();
        commands = new String[]{"CONN", "BCST", "QUIT", "PASS","AUTH", "LST", "GRP CRT", "GRP LST", "GRP EXIT", "GRP JOIN",
                "GRP BCST", "PSMG"};
    }

    public void startServer() throws IOException {
        var serverSocket = new ServerSocket(1337);

        while (true) {
            // Wait for an incoming client-connection request (blocking).
            Socket socket = serverSocket.accept();
            // Your code here:
            // TODO: Start a message processing thread for each connecting client.
            ClientHandler clientHandler = new ClientHandler(socket, this);

            clientHandler.start();

            // TODO: Start a ping thread for each connecting client.
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
                        client.setStatus("AUTHENTICATED");
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

    public void loginUser(ClientHandler user, String username) {
        boolean usernameExists = false;

        for (ClientHandler client: clientHandlers) {
            if (username.equalsIgnoreCase(client.getUsername())) {
                usernameExists = true;
            }
        }

        if (!usernameExists) {
            user.setStatus("CONNECTED");
            user.setUsername(username);
            clientHandlers.add(user);
            user.writeToClient("OK " + username);
            System.out.println("OK " + username);
        } else {
            user.writeToClient("ERR01 This username already exists");
            System.out.println("Username "+username+" already exists");
        }
    }

    public void sendBroadcastToEveryone(ClientHandler user, String message) {
        for (ClientHandler client: clientHandlers) {
            client.writeToClient(commands[1] + " " + user.getUsername() + " " + message);
        }

        user.writeToClient("OK " + commands[1] + " " + message);
    }

    //The following are methods meant for different situations

    /**
     * METHOD 1: Printing out a list of all clients connected to the server
     */
    public void listAllClients(){
        PrintWriter writer = new PrintWriter(outputStream);

        writer.println("OK LST ");
        writer.flush();

        for (ClientHandler clientHandler : clientHandlers) {

            if(clientHandler.getStatus().equals("AUTHENTICATED")) {
                writer.println(1 + " " + clientHandler.getUsername());
            }else{
                writer.println(0 + " " + clientHandler.getUsername());
            }
            writer.flush();
        }
        System.out.println("Clients listed");
    }


    /**
     * METHOD2: Creating a new group
     * @param groupName
     */
    public void createGroup(String groupName){

        Pattern pattern = Pattern.compile("[- !@#$%^&*()+=|/?.>,<`~]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(groupName);
        boolean matchFound = matcher.find();

        if(matchFound){

            PrintWriter writer = new PrintWriter(outputStream);
            writer.println("Invalid Group name, please use letters and numbers only :) e.g MangoJuju6969");
            writer.flush();

        }else{
            writer.println("OK CRT Group Created");
            writer.flush();
            System.out.println("New group: "+groupName+" created");

            Group group = new Group(groupName);
            groups.add(group);
        }
    }

    /**
     * METHOD 3: Listing all the groups connected to the server
     */
    public void listAllGroups(){

        PrintWriter writer = new PrintWriter(outputStream);


        writer.println("OK LST ");
        writer.flush();

        for (Group group:groups) {
            writer.print(group.getGroupName()+" , ");
            writer.flush();
        }
        System.out.println("Groups listed");
    }

    //THIS METHOD IS MEANT TO LIST ALL CLIENTS IN A GROUP
//    public void listAllClientsInGroup(Group groupName){
//
//        PrintWriter writer = new PrintWriter(outputStream);
//
//        if(groups.contains(groupName)){
//
//            writer.println("OK LST ");
//            writer.flush();
//
//            for (Group group:groups) {
//                writer.print(group.getClientsInGroup()+" , ");
//                writer.flush();
//            }
//
//        }
//    }

    /**
     * Method 4: Joining an existing group
     * @param groupName
     * @param clientName
     */
    public void joinGroup(String groupName, String clientName) {

        PrintWriter writer = new PrintWriter(outputStream);
//IF statement for the logged in user line.
        for (Group groups:groups) {
            if(groups.getGroupName().equals(groupName)){
                for (ClientHandler clientHandler : clientHandlers) {
                    if(clientHandler.getUsername().equals(clientName)){

                        groups.addToGroup(clientHandler);
                        writer.println("OK JOIN");
                        writer.flush();
                        System.out.println("Username "+clientName+" joined "+ groupName);
                    }
                }
            }else{
                writer.println("ER06 Group does not exist");
                System.out.println("ER06 Group does not exist");
            }
        }
    }

    /**
     * Method 5: Leaving a group chat
     * @param groupName
     * @param userName
     */
    public void leaveGroupChat(String groupName, String userName){
        PrintWriter writer = new PrintWriter(outputStream);

        for (Group group:groups) {
            if(group.getGroupName().equals(groupName)){
                for (ClientHandler clientHandler : clientHandlers) {
                    if(clientHandler.getUsername().equals(userName)){
                        group.removeFromGroup(clientHandler);
                        System.out.println("Username "+clientHandler.getUsername()+ " exited "+ groupName);
                    }
                }
            }else{
                writer.println("ER06 Group does not exist");
                System.out.println("ER06 Group does not exist");
            }
        }
    }

    public void disconnectFromServer(){
        PrintWriter writer = new PrintWriter(outputStream);
        
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
