public class Client {
    private String status;
    private String username;
    private String password;
    private boolean authenticated;

    public Client(String username) {
        this.status = null;
        this.username = username;
        this.authenticated = false;
        this.password = " ";
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

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
