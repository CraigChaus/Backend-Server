package server;

import java.io.OutputStream;
import java.io.PrintWriter;

public class PingPongThread extends Thread{
    private final OutputStream outputStream;

    public PingPongThread(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void run() {
//        while (true) {
//
//            PrintWriter writer = new PrintWriter(outputStream);
//
//            writer.println("PING");
//            writer.flush();
//            System.out.println(">>>> PING");
//
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//        }
    }
}
