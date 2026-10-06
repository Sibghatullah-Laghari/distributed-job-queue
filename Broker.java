import java.io.*;
import java.net.*;

public class Broker {

    private static final int PORT = 9090;

    private final QueueManager queueManager =
            new QueueManager();

    public void start() throws IOException {

        ServerSocket serverSocket =
                new ServerSocket(PORT);

        System.out.println(
                "Broker running on port " + PORT
        );

        while (true) {

            Socket clientSocket =
                    serverSocket.accept();

            System.out.println(
                    "Client connected: "
                    + clientSocket.getInetAddress()
            );

            try {
                Thread clientThread = new Thread(
                    () -> handleClient(clientSocket)
                );
                clientThread.start();
            }
            catch(Exception e) {
                serverSocket.close();
            }            
        }

    }

    private void handleClient(Socket socket) {

        try (
                BufferedReader in =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()
                                )
                        );

                PrintWriter out =
                        new PrintWriter(
                                socket.getOutputStream(),
                                true
                        )
        ) {

            String command;

            while ((command = in.readLine()) != null) {

                processCommand(command, out);
            }

        } catch (IOException e) {

            System.out.println(
                    "Client disconnected."
            );
        }
    }

    private void processCommand(
            String command,
            PrintWriter out
    ) {

        String[] parts =
                command.split(" ", 3);

        String operation = parts[0];

        try {

            switch (operation) {

                case "CREATE_QUEUE":

                    queueManager.createQueue(
                            parts[1]
                    );

                    out.println("QUEUE_CREATED");

                    break;

                case "PUBLISH":

                    String queueName = parts[1];

                    String message = parts[2];

                    queueManager.publish(
                            queueName,
                            new Message(message)
                    );

                    out.println("MESSAGE_PUBLISHED");

                    break;

                case "CONSUME":

                    String queue = parts[1];

                    Message msg =
                            queueManager.consume(queue);

                    out.println(
                            "MESSAGE " + msg.getContent()
                    );

                    break;

                default:

                    out.println(
                            "UNKNOWN_COMMAND"
                    );
            }

        } catch (Exception e) {

            out.println(
                    "ERROR " + e.getMessage()
            );
        }
    }

    public static void main(String[] args)
            throws IOException {

        new Broker().start();
    }
}