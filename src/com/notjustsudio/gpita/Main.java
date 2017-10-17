package com.notjustsudio.gpita;

import com.notjuststudio.fpnt.FPNTConstants;
import com.notjuststudio.fpnt.FPNTContainer;
import com.notjustsudio.gpita.network.Client;
import com.notjustsudio.gpita.network.Server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        final ExecutorService service = Executors.newFixedThreadPool(2);

        final Server server = new Server(25565)
                .printLogs(false)
                .inactive(() ->
                        connection -> System.err.println(connection.id() + " was inactivated")
                );

        final Client client = new Client("localhost", 25565)
                .printLogs(true)
                .printExceptions(true)
                .map(handlerMap ->
                        handlerMap.put("default", (connection, container) -> {
                                    System.err.println("Server send: " + container.getValue(FPNTConstants.STRING, "message"));
                                    server.shutdown();
                                }
                        )
                )
                .exception(() -> (connection, throwable) -> throwable.printStackTrace());

        server
                .active(() ->
                        connection -> {
                            System.err.println(connection.id() + " was activated");
                            final FPNTContainer container = new FPNTContainer();
                            container.putValue(FPNTConstants.STRING, "message", "Hello, i'm a client");
                            client.connection().send("default", container);
                        })
                .map(handlerMap ->
                        handlerMap.put("default", (connection, container) -> {
                            System.err.println("Client send: " + container.getValue(FPNTConstants.STRING, "message"));
                            container.putValue(FPNTConstants.STRING, "message", "HELLO, " + connection.id() + ", I'M THE GOD");
                            connection.send("default", container);
                        })
                )
                .started(() -> {
                    service.submit(client);
                    System.err.println(client.isConnected());
                })
                .stopped(service::shutdown);

        service.submit(server);
    }
}
