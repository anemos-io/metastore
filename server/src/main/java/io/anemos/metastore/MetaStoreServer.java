package io.anemos.metastore;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.config.TraceConfig;
import io.opencensus.trace.config.TraceParams;
import io.opencensus.trace.samplers.Samplers;
import java.io.IOException;
import java.util.logging.Logger;

public class MetaStoreServer {
  private static final Logger logger = Logger.getLogger(MetaStoreServer.class.getName());

  private final int port;
  private final Server server;

  /** Create a RouteGuide server listening on {@code port} using {@code featureFile} database. */
  private MetaStoreServer(int port) throws IOException {
    this(ServerBuilder.forPort(port), port);
  }

  /** Create a RouteGuide server using serverBuilder as a base and features as data. */
  private MetaStoreServer(ServerBuilder<?> serverBuilder, int port) throws IOException {
    this.port = port;
    MetaStore metaStore = new MetaStore();

    server =
        serverBuilder
            .addService(new MetaStoreService(metaStore))
            .addService(new RegistryService(metaStore))
            .addService(ProtoReflectionService.newInstance())
            .build();
  }

  /** Main method. This comment makes the linter happy. */
  public static void main(String[] args) throws Exception {

    // 2. Configure 100% sample rate, otherwise, few traces will be sampled.
    TraceConfig traceConfig = Tracing.getTraceConfig();
    TraceParams activeTraceParams = traceConfig.getActiveTraceParams();
    traceConfig.updateActiveTraceParams(
        activeTraceParams.toBuilder().setSampler(Samplers.alwaysSample()).build());

    MetaStoreServer server = new MetaStoreServer(8980);
    server.start();
    server.blockUntilShutdown();
  }

  /** Start serving requests. */
  public void start() throws IOException {
    server.start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                  System.err.println("*** shutting down gRPC server since JVM is shutting down");
                  MetaStoreServer.this.stop();
                  System.err.println("*** server shut down");
                }));
  }

  /** Stop serving requests and shutdown resources. */
  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  /** Await termination on the main thread since the grpc library uses daemon threads. */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }
}
