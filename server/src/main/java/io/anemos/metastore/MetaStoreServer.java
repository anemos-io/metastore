package io.anemos.metastore;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;

import java.io.IOException;
import java.util.logging.Logger;

public class MetaStoreServer {
    private static final Logger logger = Logger.getLogger(MetaStoreServer.class.getName());

    private final int port;
    private final Server server;

    /**
     * Create a RouteGuide server listening on {@code port} using {@code featureFile} database.
     */
    public MetaStoreServer(int port) throws IOException {
        this(ServerBuilder.forPort(port), port);
    }

    /**
     * Create a RouteGuide server using serverBuilder as a base and features as data.
     */
    public MetaStoreServer(ServerBuilder<?> serverBuilder, int port) throws IOException {
        this.port = port;
        MetaStore metaStore = new MetaStore();

        server = serverBuilder
                .addService(new MetaStoreService(metaStore))
                .addService(new SchemaRegistryService(metaStore))
                .addService(ProtoReflectionService.newInstance())
                .build();
    }

    /**
     * Main method.  This comment makes the linter happy.
     */
    public static void main(String[] args) throws Exception {
        MetaStoreServer server = new MetaStoreServer(8980);
        server.start();
        server.blockUntilShutdown();
    }

    /**
     * Start serving requests.
     */
    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                MetaStoreServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    /**
     * Stop serving requests and shutdown resources.
     */
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}