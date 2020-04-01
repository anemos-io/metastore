package io.anemos.metastore.config;

import io.anemos.metastore.core.git.GitConfig;
import java.util.HashMap;
import java.util.Map;

public class ProviderConfig {
  private boolean resolved = false;
  private Map<String, String> resolvedParameters;
  private String providerClass;
  private Parameters[] parameters;
  private GitProviderConfig git;

  public String getProviderClass() {
    if (!resolved) {
      throw new IllegalStateException();
    }
    return providerClass;
  }

  public void setProviderClass(String providerClass) {
    this.providerClass = providerClass;
  }

  public void setParameters(Parameters[] parameters) {
    this.parameters = parameters;
  }

  public void setGit(GitProviderConfig git) {
    this.git = git;
  }

  public ProviderConfig resolve(GitGlobalConfig globalGit) {
    if (resolved) {
      return this;
    }
    resolvedParameters = new HashMap<>();
    if (this.parameters != null) {
      for (ProviderConfig.Parameters parameter : this.parameters) {
        resolvedParameters.put(parameter.name, parameter.value);
      }
    }
    resolvedParameters.putAll(GitConfig.fromConfigFile(git, globalGit).toMap());
    resolved = true;
    return this;
  }

  public Map<String, String> getParameter() {
    if (!resolved) {
      throw new IllegalStateException();
    }
    return resolvedParameters;
  }

  public static class Parameters {
    public String name;
    public String value;

    public Parameters() {}

    public Parameters(String name, String value) {
      this.name = name;
      this.value = value;
    }
  }
}
