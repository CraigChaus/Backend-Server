package nl.saxion.itech;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class IntegrationAcceptedGroupname {

    private static Properties props = new Properties();

    private Socket s;
    private Socket fs;
    private BufferedReader in;
    private PrintWriter out;

    private final static int max_delta_allowed_ms = 100;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = IntegrationAcceptedUsernames.class.getResourceAsStream("testconfig.properties");
        props.load(in);
        in.close();
    }

    @BeforeEach
    void setup() throws IOException {
        s = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        fs = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("filePort")));
        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        out = new PrintWriter(s.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        s.close();
        fs.close();
    }
    @Test
    @DisplayName("RQ-B202 - groupNameThreeCharactersLongIsAllowed")
    void groupNameThreeCharactersLongIsAllowed() {
        out.println("CONN tester");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assumeTrue(serverResponse.startsWith("OK"));

        out.println("GRP CRT hsd");
        out.flush();
        serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK CRT", serverResponse);
    }

    @Test
    @DisplayName("RQ-B202 - groupNameFourteenCharactersLongIsAllowed")
    void groupNameFourteenCharactersLongIsAllowed() {
        out.println("CONN tester2");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assumeTrue(serverResponse.startsWith("OK"));

        out.println("GRP CRT hsdhgklehtundf");
        out.flush();
        serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK CRT", serverResponse);
    }

    private String receiveLineWithTimeout(BufferedReader reader){
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), () -> reader.readLine());
    }
}