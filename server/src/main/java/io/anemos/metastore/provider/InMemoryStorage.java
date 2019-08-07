package io.anemos.metastore.provider;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import java.util.List;
import java.util.Map;

public class InMemoryStorage implements StorageProvider, BindProvider {

  private ByteString storage;
  private BindDatabase bindDatabase;

  @Override
  public void initForBind(RegistryInfo registryInfo, Map<String, String> config, boolean readOnly) {
    bindDatabase = new BindDatabase();
  }

  @Override
  public void initForStorage(RegistryInfo registryInfo, Map<String, String> config) {
    this.storage = null;
  }

  @Override
  public ByteString read() {
    return storage;
  }

  @Override
  public void write(ByteString payload) {
    storage = payload;
  }

  @Override
  public void createResourceBinding(String resourceUrn, Descriptors.Descriptor descriptor) {
    bindDatabase.bind(resourceUrn, descriptor.getFullName());
  }

  @Override
  public void updateResourceBinding(String resourceUrn, Descriptors.Descriptor descriptor) {
    bindDatabase.bind(resourceUrn, descriptor.getFullName());
  }

  @Override
  public void deleteResourceBinding(String resourceUrn) {
    bindDatabase.remove(resourceUrn);
  }

  @Override
  public List<BindResult> listResourceBindings(String next_page_token) {
    return bindDatabase.list(next_page_token);
  }

  @Override
  public boolean isWriteOnly() {
    return false;
  }

  @Override
  public BindResult getResourceBinding(String resourceUrn) {
    return bindDatabase.get(resourceUrn);
  }
}
