import java.util.ArrayList;

public class Group {

    private String groupName;
    private ArrayList<Client> clientsInGroup;

    public Group(String groupName){
        this.groupName = groupName;
        this.clientsInGroup = null;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public ArrayList<Client> getClientsInGroup() {
        return clientsInGroup;
    }

    public void setClientsInGroup(ArrayList<Client> clientsInGroup) {
        this.clientsInGroup = clientsInGroup;
    }
    public void addToGroup(Client client){

        for (Client client1: clientsInGroup) {
            if(!client1.getUsername().equals(client.getUsername())){
                clientsInGroup.add(client);
            }
        }
    }
    public void removeFromGroup(Client client){
        for (Client userName1: clientsInGroup) {
            if(!userName1.getUsername().equals(client.getUsername())){
                clientsInGroup.remove(client);
            }
        }
    }
}
