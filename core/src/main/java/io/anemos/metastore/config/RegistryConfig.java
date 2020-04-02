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
    if (storage == null) {
      storage = new ArrayList<>();
    }
    if (bind == null) {
      bind = new ArrayList<>();
    }
    if (eventing == null) {
      eventing = new ArrayList<>();
    }

    // if a global storage provider is set, if will be added as first provider for
    // binding and storage providers for each registry.
    if (globalStorage != null) {
      storage.add(0, globalStorage);
      bind.add(0, globalStorage);
    }

    // if no provider is set, we default to in memory
    if (bind.size() > 0 && storage.size() == 0) {
      throw new IllegalStateException("Configuring bind providers without storage is not allowed.");
    }

    if (storage.size() == 0) {
      LOG.warn("Storage Provider not configured, defaulting to in memory provider");
      ProviderConfig entry = new ProviderConfig();
      entry.setProviderClass("io.anemos.metastore.provider.InMemoryStorage");
      storage.add(entry);
    }
    if (bind.size() == 0) {
      LOG.warn("Bind Provider not configured, defaulting to in memory provider");
      ProviderConfig entry = new ProviderConfig();
      entry.setProviderClass("io.anemos.metastore.provider.InMemoryStorage");
      bind.add(entry);
    }

    // resolve all the providers
    for (ProviderConfig providerConfig : storage) {
      providerConfig.resolve(globalGit);
    }
    for (ProviderConfig providerConfig : bind) {
      providerConfig.resolve(globalGit);
    }
    for (ProviderConfig providerConfig : eventing) {
      providerConfig.resolve(globalGit);
    }

    // init the git config
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
