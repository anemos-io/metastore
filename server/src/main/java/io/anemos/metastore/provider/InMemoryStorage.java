package io.anemos.metastore.provider;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Map;

public class InMemoryStorage implements StorageProvider {

  private HashMap<String, ByteString> storage;

  public InMemoryStorage(Map<String, String> config) {
    this.storage = new HashMap<>();
  }

  @Override
  public ByteString read(String fileName) {
    return storage.getOrDefault(fileName, null);
  }

  @Override
  public void write(String fileName, ByteString payload) {
    storage.put(fileName, payload);
  }
}
