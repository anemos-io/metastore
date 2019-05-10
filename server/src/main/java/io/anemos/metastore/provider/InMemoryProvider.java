package io.anemos.metastore.provider;

import com.google.protobuf.ByteString;
import java.util.HashMap;

public class InMemoryProvider implements MetaStoreStorageProvider {

  private HashMap<String, ByteString> storage;

  public InMemoryProvider() {
    this.storage = new HashMap<>();
  }

  @Override
  public ByteString read(String fileName) {
    return storage.getOrDefault(fileName, ByteString.EMPTY);
  }

  @Override
  public void write(String fileName, ByteString payload) {
    storage.put(fileName, payload);
  }
}
