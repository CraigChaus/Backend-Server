package client;

import client.ClientHandler;

import java.util.ArrayList;

public class Group {

    private String groupName;
    private ArrayList<ClientHandler> clientsInGroup;

    public Group(String groupName){
        this.groupName = groupName;
        this.clientsInGroup = new ArrayList<>();
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public ArrayList<ClientHandler> getClientsInGroup() {
        return clientsInGroup;
    }

    public void addToGroup(ClientHandler clientHandler){
        for (ClientHandler clientHandler1 : clientsInGroup) {
            if(!clientHandler1.getUsername().equals(clientHandler.getUsername())){
                clientsInGroup.add(clientHandler);
            }
        }
    }
    public void removeFromGroup(ClientHandler clientHandler){
        for (ClientHandler userName1: clientsInGroup) {
            if(!userName1.getUsername().equals(clientHandler.getUsername())){
                clientsInGroup.remove(clientHandler);
            }
        }
    }
}
