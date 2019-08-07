package io.anemos.metastore.provider;

import com.google.protobuf.ByteString;
import java.util.Map;

/**
 * A StorageProvider needs to implement this interface, it simply will enable storing and loading
 * the full ProtoDescriptorSet blob. This makes Metastore extensible so it can be used on any
 * platform.
 */
public interface StorageProvider {

  void initForStorage(RegistryInfo registryInfo, Map<String, String> config);

  /** Read the ProtoDescriptorSet blob from storage. */
  ByteString read();

  /** Write the ProtoDescriptorSet tot storage. */
  void write(ByteString payload);
}
