package io.anemos.metastore.config;

import io.anemos.metastore.core.git.GitConfig;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistryConfig {
  private static final Logger LOG = LoggerFactory.getLogger(RegistryConfig.class);
  private String[] scope;
  private String name;
  private String shadowOf;
  private List<ProviderConfig> storage;
  private List<ProviderConfig> bind;
  private List<ProviderConfig> eventing;
  private boolean resolved = false;
  private GitConfig gitConfig;
  private GitProviderConfig git;

  public RegistryConfig() {}

  public RegistryConfig(String name) {
    this.name = name;
  }

  public RegistryConfig(String name, String shadowOf, String[] scope) {
    this.shadowOf = shadowOf;
    this.name = name;
    this.scope = scope;
  }

  public String[] getScope() {
    return scope;
  }

  public void setScope(String[] scope) {
    this.scope = scope;
  }

  public String getShadowOf() {
    return shadowOf;
  }

  public void setShadowOf(String shadowOf) {
    this.shadowOf = shadowOf;
  }

  public List<ProviderConfig> getStorage() {
    if (!resolved) {
      throw new IllegalStateException();
    }
    return storage;
  }

  public void setStorage(List<ProviderConfig> storage) {
    this.storage = storage;
  }

  public List<ProviderConfig> getBind() {
    if (!resolved) {
      throw new IllegalStateException();
    }
    return bind;
  }

  public void setBind(List<ProviderConfig> bind) {
    this.bind = bind;
  }

  public List<ProviderConfig> getEventing() {
    if (!resolved) {
      throw new IllegalStateException();
    }
    return eventing;
  }

  public void setEventing(List<ProviderConfig> eventing) {
    this.eventing = eventing;
  }

  public GitConfig getGitConfig() {
    return gitConfig;
  }

  public void setGit(GitProviderConfig git) {
    this.git = git;
  }

  public RegistryConfig resolve(ProviderConfig globalStorage, GitGlobalConfig globalGit) {
    if (resolved) {
      return this;
    }
    if (storage == null && globalStorage == null) {
      throw new RuntimeException("No storage provider set for this registry");
    }
    if (storage == null) {
      storage = new ArrayList<>();
      if (globalStorage == null) {
        LOG.warn("Storage Provider not configured, defaulting to in memory provider");
        ProviderConfig entry = new ProviderConfig();
        entry.setProviderClass("io.anemos.metastore.provider.InMemoryStorage");
        storage.add(entry);
      }
    }
    if (globalStorage != null) {
      storage.add(0, globalStorage);
    }
    for (ProviderConfig providerConfig : storage) {
      providerConfig.resolve(globalGit);
    }

    if (bind == null) {
      bind = new ArrayList<>();
    }
    for (ProviderConfig providerConfig : bind) {
      providerConfig.resolve(globalGit);
    }
    if (eventing == null) {
      eventing = new ArrayList<>();
    }
    for (ProviderConfig providerConfig : eventing) {
      providerConfig.resolve(globalGit);
    }
    gitConfig = GitConfig.fromConfigFile(git, globalGit);
    resolved = true;
    return this;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
