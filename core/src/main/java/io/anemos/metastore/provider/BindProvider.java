package io.anemos.metastore.provider;

import com.google.protobuf.Descriptors;
import java.util.List;
import java.util.Map;

public interface BindProvider {

  void initForBind(RegistryInfo registryInfo, Map<String, String> config, boolean writeOnly);

  void createResourceBinding(String resourceUrn, Descriptors.Descriptor descriptor);

  void updateResourceBinding(String resourceUrn, Descriptors.Descriptor descriptor);

  void deleteResourceBinding(String resourceUrn);

  List<BindResult> listResourceBindings(String next_page_token);

  boolean isWriteOnly();

  BindResult getResourceBinding(String resourceUrn);
}
