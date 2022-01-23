package nl.saxion.itech;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class IntegrationGroupFunctionalities {
    private static Properties props = new Properties();

    private Socket socketUser1,socketUser2;
    private Socket fileSocket1,fileSocket2;
    private BufferedReader inUser1,inUser2;
    private PrintWriter outUser1,outUser2;

    private final static int max_delta_allowed_ms = 100;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = IntegrationMultipleUserTests.class.getResourceAsStream("testconfig.properties");
        props.load(in);
        in.close();
    }

    @BeforeEach
    void setup() throws IOException {
        //User1
        socketUser1 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        fileSocket1 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("filePort")));
        inUser1 = new BufferedReader(new InputStreamReader(socketUser1.getInputStream()));
        outUser1 = new PrintWriter(socketUser1.getOutputStream(), true);

        //User2
        socketUser2 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        fileSocket2 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("filePort")));
        inUser2 = new BufferedReader(new InputStreamReader(socketUser2.getInputStream()));
        outUser2 = new PrintWriter(socketUser2.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        socketUser1.close();
        fileSocket1.close();
        socketUser2.close();
        fileSocket2.close();
    }

    /**
     * List all the groups , good weather test
     */
    @Test
    @DisplayName("RQ-G202 listAllGroupsGood ")
    void listAllGroupsGood() {
        // Connect user 1
        outUser1.println("CONN groupcreator");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //send GRP CRT from groupcreator
        outUser1.println("GRP CRT group1");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Send GRP CRT from groupcreator again
        outUser1.println("GRP CRT group2");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Request list of groups from groupcreator
        outUser1.println("GRP LST groupcreator");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("OK GRP LST group1 , group2 , ", resUser1);
    }

    /**
     * List all the groups without logging in, good weather test
     */
    @Test
    @DisplayName("RQ-G202 listAllGroupsBad1 without logging in first ")
    void listAllGroupsBad1() {
        // Connect user 1
        outUser1.println("CONN correctGroupcreator");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //send GRP CRT from correctGroupcreator
        outUser1.println("GRP CRT group3");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Send GRP CRT from correctGroupcreator again
        outUser1.println("GRP CRT group4");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Request list of groups from NOTLOGGEDINUSER (notLoggedIn)
        outUser2.println("GRP LST notLoggedIn");
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2); //server 200 response
        assertEquals("ERR03 Please log in first", resUser2);
    }

    /**
     * List all the groups when no group is present is allowed, good weather test
     */
    @Test
    @DisplayName("RQ-G202 listAllGroupsGood1 with no groups created is allowed ")
    void listAllGroupsGood1() {
        // Connect user 1
        outUser1.println("CONN justAUser");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Request empty list of groups from justAUser
        outUser1.println("GRP LST justAUser");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("OK GRP LST ", resUser1);
    }

    /**
     * Join a group that does exist, good weather
     */
    @Test
    @DisplayName("RQ-G202 join a group that exists - good weather")
    void joinGroupGood() {
        // Connect user 1
        outUser1.println("CONN groupjoiner");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //send GRP CRT from groupjoiner
        outUser1.println("GRP CRT group98");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Request to join group98 from groupjoiner
        outUser1.println("GRP JOIN group98 groupjoiner");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("OK JOIN group98", resUser1);
    }

    /**
     * Try to join a group that does not exist
     */
    @Test
    @DisplayName("RQ-G202 join a group that does not exist - bad weather")
    void joinGroupBad2() {
        // Connect groupjoiner
        outUser1.println("CONN groupjoiner");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //send GRP CRT from groupjoiner
        outUser1.println("GRP CRT group98");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Request to join group102 (does not exist) from groupjoiner
        outUser1.println("GRP JOIN group102 groupjoiner");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("ERR06 Group does not exist", resUser1);
    }

    /**
     * Join a group then leave that group
     */
    @Test
    @DisplayName("RQ-G202 join a group then leave it - good weather")
    void leaveGroupChatGood() {
// Connect user 1
        outUser1.println("CONN groupcreator");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //send GRP CRT from groupcreator
        outUser1.println("GRP CRT group1ToLeave");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Send GRP Join to group1ToLeave from groupcreator
        outUser1.println("GRP JOIN group1ToLeave groupjoiner");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Request leaving group group1ToLeave
        outUser1.println("GRP EXIT group1ToLeave groupjoiner");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("OK EXIT", resUser1);
    }

    /**
     * Join a group then leave it then leave it again
     */
    @Test
    @DisplayName("RQ-G202 join a group then leave it then leave it again - bad weather")
    void leaveGroupChatBad() {
// Connect user 1
        outUser1.println("CONN groupcreator");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //send GRP CRT from groupcreator
        outUser1.println("GRP CRT group1ToLeave");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Send GRP Join to group1ToLeave from groupcreator
        outUser1.println("GRP JOIN group1ToLeave groupjoiner");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Request leaving group group1ToLeave
        outUser1.println("GRP EXIT group1ToLeave groupjoiner");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("OK EXIT", resUser1);

        //Request leaving group group1ToLeave
        outUser1.println("GRP EXIT group1ToLeave groupjoiner");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("ERR08 You are not a member of this group", resUser1);
    }

    /**
     * Join a group then join it again
     */
    @Test
    @DisplayName("RQ-G202 join a group then join it again - bad weather")
    void leaveGroupChatBad2() {
        // Connect user 1
        outUser1.println("CONN groupcreator");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //send GRP CRT from groupcreator
        outUser1.println("GRP CRT group1ToJoin");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Send GRP Join to group1ToJoin from groupcreator
        outUser1.println("GRP JOIN group1ToJoin groupjoiner");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Send GRP Join again to group1ToJoin from groupcreator
        outUser1.println("GRP JOIN group1ToJoin groupjoiner");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("ERR07 You are already in the group", resUser1);
    }

    /**
     * Send as message to an existing group, good weather test
     */
    @Test
    @DisplayName("RQ-G202 sendMessageToGroupGood")
    void sendMessageToGroupGood() {
        // Connect user 1
        outUser1.println("CONN groupcreator");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //send GRP CRT from groupcreator to create group1
        outUser1.println("GRP CRT group1");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Send GRP Join to group1 from groupcreator
        outUser1.println("GRP JOIN group1 groupcreator");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Send GRP BCST from groupcreator to group1
        outUser1.println("GRP BCST group1 hello");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("GRP BCST group1 groupcreator hello", resUser1);
    }

    /**
     * Send as message to a group that doesnt exist, bad weather test
     */
    @Test
    @DisplayName("RQ-G202 sendMessageToGroupBad")
    void sendMessageToGroupBad() {
        // Connect user 1
        outUser1.println("CONN groupcreator");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //send GRP CRT from groupcreator to create group1
        outUser1.println("GRP CRT group1");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Send GRP BCST from groupcreator to group5
        outUser1.println("GRP BCST group5 hello");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("ERR06 Group does not exist", resUser1);
    }

    /**
     * Send as message to a group, without logging in bad weather test
     */
    @Test
    @DisplayName("RQ-G202 sendMessageToGroupBad2 without logging in")
    void sendMessageToGroupBad2() {
        // Connect user 1
        outUser1.println("CONN groupcreator");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //send GRP CRT from groupcreator to create group1
        outUser1.println("GRP CRT group1");
        outUser1.flush();
        resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        //Send GRP BCST from groupcreator to group5
        outUser2.println("GRP BCST group5 hello");
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2); //server 200 response
        assertEquals("ERR03 Please log in first", resUser2);
    }

    private String receiveLineWithTimeout(BufferedReader reader){
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), () -> reader.readLine());
    }
}