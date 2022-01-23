package nl.saxion.itech;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class IntegrationMultipleUserTests {

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
     * Test to send broadcast message, good weather, BCST
     */
    @Test
    @DisplayName("RQ-U101 - BCSTGoodMessage")
    void BCSTGoodMessage() {
        // Connect user 1
        outUser1.println("CONN user1");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        // Connect user 2
        outUser2.println("CONN user2");
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2); //server 200 response
        assumeTrue(resUser2.startsWith("OK"));

        //send BCST from USER1
        outUser1.println("BCST messagefromuser1");
        outUser1.flush();
        String fromUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("OK BCST messagefromuser1", fromUser1);

        String fromUser2 = receiveLineWithTimeout(inUser2); //BCST from user1
        assertEquals("BCST user1 messagefromuser1", fromUser2);

        //send BCST from USER2
        outUser2.println("BCST messagefromuser2");
        outUser2.flush();
        fromUser2 = receiveLineWithTimeout(inUser2); //server 200 response
        assertEquals("OK BCST messagefromuser2", fromUser2);

        fromUser1 = receiveLineWithTimeout(inUser1); //BCST from user2
        assertEquals("BCST user2 messagefromuser2", fromUser1);
    }

    /**
     * Test to send broadcast message without logging in first, bad weather, BCST
     */
    @Test
    @DisplayName("RQ-U101 - BCSTBadMessage")
    void BCSTBadMessage() {
        // Connect user 3
        outUser2.println("CONN user3");
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2); //server 200 response
        assumeTrue(resUser2.startsWith("OK"));

        //send BCST from USER4
        outUser1.println("BCST messagefromuser4");
        outUser1.flush();
        String fromUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("ERR03 Please log in first", fromUser1);
    }

    /**
     * Test to send private message,good weather PMSG
     */
    @Test
    @DisplayName("RQ-U101 - PMSGGoodMessage")
    void PMSGGoodMessage() {
        // Connect user 5
        outUser1.println("CONN user5");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        // Connect user 6
        outUser2.println("CONN user6");
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2); //server 200 response
        assumeTrue(resUser2.startsWith("OK"));

        //send PMSG from USER5 to USER6
        outUser1.println("PMSG user6 messagefromuser5");
        outUser1.flush();
        String fromUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("OK PMSG", fromUser1);

        String fromUser2 = receiveLineWithTimeout(inUser2); //PMSG from user5
        assertEquals("PMSG user5 messagefromuser5", fromUser2);

        //send PMSG from USER6 to USER5
        outUser2.println("PMSG user5 messagefromuser6");
        outUser2.flush();
        fromUser2 = receiveLineWithTimeout(inUser2); //server 200 response
        assertEquals("OK PMSG", fromUser2);

        fromUser1 = receiveLineWithTimeout(inUser1); //PMSG from user2
        assertEquals("PMSG user6 messagefromuser6", fromUser1);
    }

    /**
     * Test to send private message to non-existent client,bad weather PMSG
     */
    @Test
    @DisplayName("RQ-U101 - PMSGBadMessage")
    void PMSGBadMessage() {
        // Connect user 7
        outUser1.println("CONN user7");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        // Connect user 8
        outUser2.println("CONN user8");
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2); //server 200 response
        assumeTrue(resUser2.startsWith("OK"));

        //send PMSG from USER7 to USER9 who doesn't exist
        outUser1.println("PMSG user101 messagefromuser7");
        outUser1.flush();
        String fromUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("ERR09 Username does not exist", fromUser1);
    }

    /**
     * Test to send private message without logging in first,bad weather PMSG
     */
    @Test
    @DisplayName("RQ-U101 - PMSGBad2Message")
    void PMSGBad2Message() {
        // Connect user 9
        outUser2.println("CONN user9");
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2); //server 200 response
        assumeTrue(resUser2.startsWith("OK"));

        //send PMSG from NOT_LOGGED_IN to USER9
        outUser1.println("PMSG user9 messagefromuser9");
        outUser1.flush();
        String fromUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("ERR03 Please log in first", fromUser1);
    }


    /**
     * Test to send private message to themselves,bad weather PMSG
     */
    @Test
    @DisplayName("RQ-U101 - PMSGBad3Message")
    void PMSGBad3Message() {
        // Connect user 72
        outUser1.println("CONN user72");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        // Connect user 90
        outUser2.println("CONN user90");
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2); //server 200 response
        assumeTrue(resUser2.startsWith("OK"));

        //send PMSG from USER72 to USER72
        outUser1.println("PMSG user72 messagefromuser72");
        outUser1.flush();
        String fromUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("ERR18 You cannot send anything to yourself", fromUser1);
    }

    /**
     * Test to connect user again after already connecting, Bad weather
     */
    @Test
    @DisplayName("RQ-S100 - Bad Weather - userAlreadyLoggedIn")
    void userAlreadyLoggedIn(){
        // Connect user 1
        outUser1.println("CONN user11");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        // Connect using same username
        outUser2.println("CONN user11");
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2);
        assertEquals("ERR01 This username already exists, choose another", resUser2);
    }

    /**
     * Test to retrieve client names,good weather LST
     */
    @Test
    @DisplayName("RQ-U101 - LSTGoodMessage")
    void LSTGoodMessage() {
        // Connect user 50
        outUser1.println("CONN user50");
        outUser1.flush();
        String resUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assumeTrue(resUser1.startsWith("OK"));

        // Connect user 60
        outUser2.println("CONN user60");
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2); //server 200 response
        assumeTrue(resUser2.startsWith("OK"));

        //send LST from
        outUser1.println("LST user60");
        outUser1.flush();
        String fromUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("OK LST 0 user62,0 user50,0 user60", fromUser1); //user 62 is coming from LSTGBadMessage
    }

    /**
     * Test to retrieve client names without logging in,Bad weather LST
     */
    @Test
    @DisplayName("RQ-U101 - LSTGBadMessage")
    void LSTBadMessage() {

        // Connect user 62
        outUser2.println("CONN user62"); //will also appear in LSTGoodMessage
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2); //server 200 response
        assumeTrue(resUser2.startsWith("OK"));

        //send LST from userNOTLOGGEDIN (user58)
        outUser1.println("LST user58");
        outUser1.flush();
        String fromUser1 = receiveLineWithTimeout(inUser1); //server 200 response
        assertEquals("ERR03 Please log in first", fromUser1);
    }

    private String receiveLineWithTimeout(BufferedReader reader){
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), () -> reader.readLine());
    }

}