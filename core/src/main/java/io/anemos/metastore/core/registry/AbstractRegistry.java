package io.anemos.metastore.core.registry;

import com.google.protobuf.ByteString;
import io.anemos.metastore.config.GitGlobalConfig;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.core.proto.PContainer;
import io.anemos.metastore.provider.StorageProvider;
import java.util.logging.Logger;

public abstract class AbstractRegistry {

  private static final Logger LOG = Logger.getLogger(AbstractRegistry.class.getName());
  private MetaGit metaGit;
  protected final Registries registries;
  protected final String name;
  RegistryConfig config;
  final StorageProvider storageProvider;
  PContainer protoContainer;

  AbstractRegistry(
      StorageProvider storageProvider,
      Registries registries,
      RegistryConfig config,
      GitGlobalConfig global) {
    this.storageProvider = storageProvider;
    this.registries = registries;
    this.name = config.name;
    this.config = config;
    this.metaGit = new MetaGit(config, global);
  }

  public abstract void init();

  public abstract void update();

  public abstract ByteString raw();

  public abstract PContainer get();

  public abstract PContainer ref();

  public abstract boolean isShadow();

  public abstract void update(PContainer ref, PContainer in);

  void syncGitRepo(String message) {
    metaGit.sync(protoContainer, message);
  }

  void initGitRepo() {
    metaGit.init();
  }
}
