package io.anemos.metastore.core.registry;

import com.google.protobuf.ByteString;
import io.anemos.metastore.config.GitGlobalConfig;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.core.proto.PContainer;
import io.anemos.metastore.provider.StorageProvider;
import java.io.IOException;

class SchemaRegistry extends AbstractRegistry {
  private final StorageProvider storageProvider;
  private final String name;

  public SchemaRegistry(
      StorageProvider storageProvider,
      Registries registries,
      RegistryConfig config,
      GitGlobalConfig global) {
    super(storageProvider, registries, config, global);
    this.storageProvider = storageProvider;
    this.name = config.name;
  }

  @Override
  public void init() {
    if (read()) {
      write();
    }
    initGitRepo();
    syncGitRepo("Initialising repository");
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
  public PContainer ref() {
    return protoContainer;
  }

  @Override
  public boolean isShadow() {
    return true;
  }

  @Override
  public void update(PContainer ref, PContainer in) {
    protoContainer = in;
    update();
    syncGitRepo("Change detected");
  }

  @Override
  public void update() {
    write();
    registries.notifyShadows(this.name);
  }

  void write() {
    storageProvider.write(name + ".pb", raw());
  }

  private boolean read() {
    try {
      ByteString buffer = storageProvider.read(name + ".pb");
      if (buffer == null) {
        this.protoContainer = new PContainer();
        return true;
      } else {
        this.protoContainer = new PContainer(buffer);
        return false;
      }
    } catch (IOException e) {
      throw new RuntimeException("failed to read default.pb", e);
    }
  }
}
