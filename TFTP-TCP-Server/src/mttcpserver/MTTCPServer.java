package mttcpserver;

import java.net.*;
import java.io.*;

public class MTTCPServer {

    public static void main(String[] args) throws IOException {

        int portNumber = 69;
        ServerSocket masterSocket;
        Socket slaveSocket;
        masterSocket = new ServerSocket(portNumber);
        System.out.println("Server Started...");
        
        while (true) {
            slaveSocket = masterSocket.accept();
            
            System.out.println("Accepted TCP connection from: " + slaveSocket.getInetAddress() + ", " + slaveSocket.getPort() + "...");
            
            new MTTCPServerThread(slaveSocket).start();
        }
    }
}
