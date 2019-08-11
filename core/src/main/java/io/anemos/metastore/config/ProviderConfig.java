package io.anemos.metastore.config;

import java.util.HashMap;
import java.util.Map;

public class ProviderConfig {

  public String providerClass;
  public Parameters[] parameters;

  public static class Parameters {
    public String name;
    public String value;

    public Parameters() {}

    public Parameters(String name, String value) {
      this.name = name;
      this.value = value;
    }
  }

  public Map<String, String> getParameter() {
    Map<String, String> parameters = new HashMap<>();
    if (this.parameters != null) {
      for (ProviderConfig.Parameters parameter : this.parameters) {
        parameters.put(parameter.name, parameter.value);
      }
    }
    return parameters;
  }
}
