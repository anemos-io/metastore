package io.anemos.metastore.provider;

public class BindResult {
  public String getLinkedResource() {
    return linkedResource;
  }

  public String getMessageName() {
    return messageName;
  }

  private String messageName;
  private String linkedResource;

  public BindResult(String linkedResource, String messageName) {
    this.linkedResource = linkedResource;
    this.messageName = messageName;
  }
}
