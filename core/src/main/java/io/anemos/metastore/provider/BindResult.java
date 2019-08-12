package io.anemos.metastore.provider;

public class BindResult {
  public String getLinkedResource() {
    return linkedResource;
  }

  public String getMessageName() {
    return messageName;
  }

  public String getServiceName() {
    return serviceName;
  }

  private String linkedResource;
  private String messageName;
  private String serviceName;

  public BindResult(String linkedResource, String messageName, String serviceName) {
    this.linkedResource = linkedResource;
    this.messageName = messageName;
    this.serviceName = serviceName;
  }
}
