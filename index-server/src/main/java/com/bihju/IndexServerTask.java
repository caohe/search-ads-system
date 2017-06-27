package com.bihju;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Log4j
@Component
public class IndexServerTask {
    private Server server;
    private IndexServerImpl indexServerImpl;

    @Value("${server.port}")
    private int serverPort;

    @Autowired
    public IndexServerTask(IndexServerImpl indexServerImpl) {
        this.indexServerImpl = indexServerImpl;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(serverPort)
                .addService(indexServerImpl)
                .build()
                .start();
        log.info("Server started, listening to port: " + serverPort);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.error("Shutting down gRPC server because JVM is shutting down");
                IndexServerTask.this.stop();
                log.error("Index server shuts down");
            }
        });

        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
}
