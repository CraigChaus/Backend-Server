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
}
