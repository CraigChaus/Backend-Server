package client;

import server.FileTransferHandler;
import server.Server;

import java.io.*;
import java.net.ServerSocket;
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


    private Server server;
    private FileTransferHandler fileTransferHandler;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader serverReader;
    private PrintWriter writer;
    private String receivedMessage;

    public ClientHandler(Socket socket, Server server) {
        this.status = null;
        this.username = "";
        this.password = "";
        this.socket = socket;
        this.server = server;
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

    public void processMessage(String message) {
        String[] command = parseMessage(message);

        switch (command[0]) {
            case "CONN":
                String username = command[1];

                boolean isUsernameAcceptable = checkUsername(username);

                if (!isUsernameAcceptable) {
                    writeToClient("ERR02 Username has an invalid format (only characters, numbers and underscores are allowed)");
                } else {
                    server.loginUser(this, username);
                }
                break;

            case "BCST":

                if (checkIfLoggedIn()) {
                    String userMessage = command[1];

                    server.sendBroadcastToEveryone(this, userMessage);
                }

                break;

            case "LST":

                if (checkIfLoggedIn()) {
                    server.listAllClients(this);
                }

                break;

            case "GRP CRT":
                if (checkIfLoggedIn()) {
                    server.createGroup(command[1], this);
                }

                break;

            case "GRP JOIN":
                if (checkIfLoggedIn()) {
                    server.joinGroup(command[1], this);
                }

                break;

            case "GRP LST":
                if (checkIfLoggedIn()) {
                    server.listAllGroups(this);
                }

                break;

            case "GRP BCST":
                if (checkIfLoggedIn()) {
                    server.sendBroadcastToGroup(this, command[1], command[2]);
                }

            case "GRP EXIT":
                if (checkIfLoggedIn()) {
                    server.leaveGroupChat(command[1], this);
                }

            case "PMSG":
                if (checkIfLoggedIn()) {
                    server.sendPrivateMessage(this, command[1], command[2]);
                }

            case "FIL ACK":
                if(checkIfLoggedIn()){
                    server.sendAcknowledgement(this, command[1]);
                }

                break;

            case "ACC":
                server.respondToAck(this, message.split(" ")[1], "ACC");

                break;

            case "DEC":
                server.respondToAck(this, message.split(" ")[1], "DEC");
                break;

            case "PASS":
                try {
                    server.createPassword(message.split(" ")[1], this);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    e.printStackTrace();
                }
                break;

            case "AUTH":
                server.authenticateMe(message.split(" ")[1], this);
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

    public String getChecksum(String filepath) throws IOException, NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("MD5");
        // DigestInputStream is better, but you also can hash file like this.
        try (InputStream fis = new FileInputStream(filepath)) {
            byte[] buffer = new byte[1024];
            int readNo;
            while ((readNo = fis.read(buffer)) != -1) {
                md.update(buffer, 0, readNo);
            }
        }
        // bytes to hex
        StringBuilder result = new StringBuilder();
        for (byte b : md.digest()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public boolean checksumFileCheck(String senderChecksum,String receiverChecksum){
        return senderChecksum.equals(receiverChecksum);
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
