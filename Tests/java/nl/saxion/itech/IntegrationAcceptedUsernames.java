package nl.saxion.itech;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationAcceptedUsernames {

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
    @DisplayName("RQ-B202 - threeCharactersIsAllowed")
    void threeCharactersIsAllowed() {
        out.println("CONN mym");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK CONN mym", serverResponse);
    }

    @Test
    @DisplayName("RQ-B202 - fourteenCharactersIsAllowed")
    void fourteenCharactersIsAllowed() {
        out.println("CONN abcdefghijklmn");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK CONN abcdefghijklmn", serverResponse);
    }

    @Test
    @DisplayName("RQ-B202 - starIsNotAllowed")
    void starIsNotAllowed() {
        out.println("CONN a*lmn");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertTrue(serverResponse.startsWith("ERR02"), "Name has an invalid format or is empty(only characters, numbers and underscores are allowed)");
    }

    private String receiveLineWithTimeout(BufferedReader reader){
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), () -> reader.readLine());
    }

}