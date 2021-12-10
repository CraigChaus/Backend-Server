import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
    private ArrayList<Client> clients;
    private  ArrayList<Group> groups;
    private String[] commands;
    private boolean loggedIn;

    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader serverReader;
    private PrintWriter writer;

    public Server() {
        clients = new ArrayList<>();
        commands = new String[]{"CONN", "BCST", "QUIT", "AUTH", "LST", "GRP CRT", "GRP LST", "GRP EXIT", "GRP JOIN",
                "GRP BCST", "PSMG"};
    }

    public void startServer() throws IOException {
        var serverSocket = new ServerSocket(1337);
        while (true) {
            // Wait for an incoming client-connection request (blocking).
            Socket socket = serverSocket.accept();
            // Your code here:
            // TODO: Start a message processing thread for each connecting client.
            Thread messageProcessingThread = new Thread(() -> {
                try {
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();

                    serverReader = new BufferedReader(new InputStreamReader(inputStream));
                    writer = new PrintWriter(outputStream);

                    String receivedMessage = serverReader.readLine();


                } catch (IOException e) {
                    e.printStackTrace();
                }

            });

            messageProcessingThread.start();
            // TODO: Start a ping thread for each connecting client.
        }
    }

    public void processMessage(String message) {
        String[] command = parseMessage(message);


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
                commandAndMessage = new String[]{command, message.split(" ")[1]};
                break;
        }

        return commandAndMessage;

    }

    //The following are methods meant for different situations

    /**
     * METHOD 1: Printing out a list of all clients connected to the server
     */
    public void listAllClients(){
        PrintWriter writer = new PrintWriter(outputStream);

        writer.println("OK LST ");
        writer.flush();

        for (Client client:clients) {

            writer.println(client.getUsername());
            writer.flush();
        }
    }//Include error message after making the login work


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
                for (Client client:clients) {
                    if(client.getUsername().equals(clientName)){

                        groups.addToGroup(client);
                        writer.println("OK JOIN");
                        writer.flush();
                    }
                }
            }else{
                writer.println("ER06 Group does not exist");
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
                for (Client client:clients) {
                    if(client.getUsername().equals(userName)){
                        group.removeFromGroup(client);
                    }
                }
            }else{
                writer.println("ER06 Group does not exist");
            }
        }
    }
    public ArrayList<Client> getClients() {
        return clients;
    }

    public void setClients(ArrayList<Client> clients) {
        this.clients = clients;
    }

    public ArrayList<Group> getGroups() {
        return groups;
    }

    public void setGroups(ArrayList<Group> groups) {
        this.groups = groups;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
}
