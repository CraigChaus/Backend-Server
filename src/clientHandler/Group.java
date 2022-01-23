package clientHandler;

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

    public ArrayList<ClientHandler> getClientsInGroup() {
        return clientsInGroup;
    }

    /**
     * Method to check if client is in the created group
     * @param clientHandler name of the client
     * @return
     */
    public boolean addToGroup(ClientHandler clientHandler){
        if (!clientsInGroup.contains(clientHandler)) {
            clientsInGroup.add(clientHandler);
            return true;
        }
        return false;
    }

    public boolean removeFromGroup(ClientHandler clientHandler){
        return clientsInGroup.remove(clientHandler);
    }
}
