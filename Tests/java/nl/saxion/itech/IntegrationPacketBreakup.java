package nl.saxion.itech;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class IntegrationPacketBreakup {

    private static Properties props = new Properties();

    private Socket s;
    private Socket fs;
    private BufferedReader in;
    private PrintWriter out;

    private final static int max_delta_allowed_ms = 100;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = IntegrationPacketBreakup.class.getResourceAsStream("testconfig.properties");
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
    @DisplayName("RQ-B202 - flushingMultipleTimesIsNotAllowed")
    void flushingMultipleTimesIsNotAllowed() {
        out.print("CONN m");
        out.flush();
        out.print("yname\r\nBC");
        out.flush();
        out.print("ST a\r\n");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals("OK CONN myname", serverResponse);
        serverResponse = receiveLineWithTimeout(in);
        assertEquals("ERR00 Unknown command", serverResponse);
    }

    private String receiveLineWithTimeout(BufferedReader reader){
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), () -> reader.readLine());
    }

}