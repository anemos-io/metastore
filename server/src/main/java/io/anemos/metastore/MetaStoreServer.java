package io.anemos.metastore;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.config.TraceConfig;
import io.opencensus.trace.config.TraceParams;
import io.opencensus.trace.samplers.Samplers;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaStoreServer {
  private static final Logger LOG = LoggerFactory.getLogger(MetaStoreServer.class);

  private final int port;
  private final Server server;

  /** Create a RouteGuide server listening on {@code port} using {@code featureFile} database. */
  private MetaStoreServer(String configPath, int port) throws IOException {
    this(configPath, ServerBuilder.forPort(port), port);
  }

  /** Create a RouteGuide server using serverBuilder as a base and features as data. */
  private MetaStoreServer(String configPath, ServerBuilder<?> serverBuilder, int port)
      throws IOException {
    MetaStore metaStore = new MetaStore(configPath);
    this.port = port;

    server =
        serverBuilder
            .addService(new MetaStoreService(metaStore))
            .addService(new RegistryService(metaStore))
            .addService(new BindService(metaStore))
            .addService(ProtoReflectionService.newInstance())
            .build();
  }

  private static void printVersion() {
    Properties properties = new Properties();
    try {
      InputStream stream = MetaStoreServer.class.getResourceAsStream("version.properties");
      if (stream != null) {
        properties.load(stream);
        LOG.info("Version: " + properties.getProperty("version"));
        LOG.info("Build Time: " + properties.getProperty("build"));
      } else {
        LOG.warn("LOCAL BUILD");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Main method. This comment makes the linter happy. */
  public static void main(String[] args) throws Exception {
    LOG.info("MetaStore server");
    printVersion();

    ArgumentParser parser = ArgumentParsers.newFor("metastore").build();
    parser.addArgument("-c", "--config").required(false);

    Namespace res = parser.parseArgs(args);
    String configPath = res.getString("config");
    if (configPath == null) {
      LOG.info("No configuration file set via argument, setting from environment.");
      configPath = System.getenv("METASTORE_CONFIG_PATH");
      LOG.info("Taking configuration file: " + configPath);
    } else {
      LOG.info("Taking configuration file: " + configPath);
    }

    // 2. Configure 100% sample rate, otherwise, few traces will be sampled.
    TraceConfig traceConfig = Tracing.getTraceConfig();
    TraceParams activeTraceParams = traceConfig.getActiveTraceParams();
    traceConfig.updateActiveTraceParams(
        activeTraceParams.toBuilder().setSampler(Samplers.alwaysSample()).build());

    String port = System.getenv("PORT");
    if (port == null) {
      port = "8980";
    }
    MetaStoreServer server = new MetaStoreServer(configPath, Integer.valueOf(port));
    server.start();
    server.blockUntilShutdown();
  }

  /** Start serving requests. */
  public void start() throws IOException {
    server.start();
    LOG.info("Server started, listening on " + port);
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
