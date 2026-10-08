package com.sibghatullah.distributedqueue;

import java.io.*;
import java.net.*;

public class Producer {

    public static void main(String[] args)
            throws IOException {

        Socket socket = new Socket("localhost", 9090);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream()));

        PrintWriter out = new PrintWriter(
                socket.getOutputStream(),
                true);

        out.println("CREATE_QUEUE emails");

        System.out.println(in.readLine());

        for (int i = 1; i <= 5; i++) {

            out.println(
                    "PUBLISH emails Message-" + i);

            System.out.println(in.readLine());
        }

        socket.close();
    }
}