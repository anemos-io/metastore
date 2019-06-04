package io.anemos.metastore.core.registry;

import com.google.protobuf.ByteString;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.core.proto.PContainer;
import io.anemos.metastore.provider.StorageProvider;
import io.anemos.metastore.v1alpha1.Report;
import java.io.IOException;

public class SchemaRegistry extends AbstractRegistry {
  private final StorageProvider storageProvider;
  private final String name;

  public SchemaRegistry(
      StorageProvider storageProvider, Registries registries, RegistryConfig config) {
    super(storageProvider, registries, config);
    this.storageProvider = storageProvider;
    this.name = config.name;
  }

  @Override
  public void init() {
    read();
  }

  @Override
  public ByteString raw() {
    return protoContainer.toByteString();
  }

  @Override
  public PContainer get() {
    return protoContainer;
  }

  @Override
  public void update(Report report, PContainer in) {
    protoContainer = in;
    storageProvider.write(name + ".pb", raw());
    syncGitRepo();
    update();
  }

  @Override
  public void update() {
    registries.notifyShadows(this.name);
  }

  private void read() {
    try {
      ByteString buffer = storageProvider.read(name + ".pb");
      if (buffer == null) {
        this.protoContainer = new PContainer();
      } else {
        this.protoContainer = new PContainer(buffer);
      }
    } catch (IOException e) {
      throw new RuntimeException("failed to read default.pb", e);
    }
  }
}
