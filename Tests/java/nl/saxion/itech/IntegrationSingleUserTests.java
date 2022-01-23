package nl.saxion.itech;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class IntegrationSingleUserTests {

    private static Properties props = new Properties();
    private static int ping_time_ms;
    private static int ping_time_ms_delta_allowed;
    private final static int max_delta_allowed_ms = 100;

    private Socket s,s1;
    private Socket fs,fs1;
    private BufferedReader in,in1;
    private PrintWriter out,out1;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = IntegrationSingleUserTests.class.getResourceAsStream("testconfig.properties");
        props.load(in);
        in.close();

        ping_time_ms = Integer.parseInt(props.getProperty("ping_time_ms", "10000"));
        ping_time_ms_delta_allowed = Integer.parseInt(props.getProperty("ping_time_ms_delta_allowed", "100"));
    }

    @BeforeEach
    void setup() throws IOException {
        s = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        fs = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("filePort")));
        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        out = new PrintWriter(s.getOutputStream(), true);

        s1 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        fs1 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("filePort")));
        in1 = new BufferedReader(new InputStreamReader(s1.getInputStream()));
        out1 = new PrintWriter(s1.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        s.close();
        fs.close();
        s1.close();
        fs1.close();
    }

    /**
     * normal login
     */
    @Test
    @DisplayName("RQ-U100 - loginSucceedsWithOK")
    void loginSucceedsWithOK() {
        out.println("CONN myname123");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK CONN myname123", serverResponse);
    }

    /**
     * log in then disconnect
     */
    @Test
    @DisplayName("RQ-U100  - LoginThenQuit")
    void LoginThenQuit() {
        out.println("CONN userToDisconnect");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assumeTrue(serverResponse.startsWith("OK"));

        out.println("QUIT");
        out.flush();
        serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK QUIT", serverResponse);
    }

    /**
     * Login with invalid characters
     */
    @Test
    @DisplayName("RQ-U100 - Bad Weather - loginInvalidCharactersWithER02")
    void loginInvalidCharactersWithER02(){
        out.println("CONN *a*");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("ERR02 Name has an invalid format or is empty(only characters, numbers and underscores are allowed)", serverResponse);
    }

    private String receiveLineWithTimeout(BufferedReader reader){
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), () -> reader.readLine());
    }
}