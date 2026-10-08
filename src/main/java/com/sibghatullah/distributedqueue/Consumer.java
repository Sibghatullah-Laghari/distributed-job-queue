package com.sibghatullah.distributedqueue;

import java.io.*;
import java.net.*;

public class Consumer {

    public static void main(String[] args)
            throws IOException {

        Socket socket = new Socket("localhost", 9090);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream()));

        PrintWriter out = new PrintWriter(
                socket.getOutputStream(),
                true);

        while (true) {

            out.println("CONSUME emails");

            String response = in.readLine();

            if (response == null) {
                break;
            }

            System.out.println(
                    "Consumer "
                            + Thread.currentThread().getName()
                            + ": "
                            + response);
        }

        socket.close();
    }
}