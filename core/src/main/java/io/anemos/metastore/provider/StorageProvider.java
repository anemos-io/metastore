package io.anemos.metastore.provider;

import com.google.protobuf.ByteString;

public interface StorageProvider {

  ByteString read(String fileName);

  void write(String fileName, ByteString payload);
}
