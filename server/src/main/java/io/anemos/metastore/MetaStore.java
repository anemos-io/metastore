package io.anemos.metastore;

import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.provider.MetaStoreStorageProvider;
import io.anemos.metastore.v1alpha1.Report;
import java.io.IOException;
import java.util.logging.Logger;

public class MetaStore {
  private static final Logger logger = Logger.getLogger(MetaStore.class.getName());

  private MetaStoreStorageProvider provider;

  //    MonoRegistry registry;
  public ProtoDescriptor repo;
  public ShadowRegistry shadowRegistry;

  /** Create a RouteGuide server listening on {@code port} using {@code featureFile} database. */
  public MetaStore() throws IOException {

    String providerClass = System.getenv("METASTORE_STORAGE_PROVIDER");
    if (providerClass == null) {
      providerClass = "InMemoryProvider";
    }
    try {
      provider =
          (MetaStoreStorageProvider) Class.forName(providerClass).getConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    repo = new ProtoDescriptor(provider.read("default.pb").toByteArray());
    Report shadowDelta = Report.parseFrom(provider.read("shadow.pb"));
    shadowRegistry = new ShadowRegistry(this, shadowDelta);
  }

  public void writeDefault() {
    provider.write("default.pb", repo.toByteString());
  }

  public void writeShadowDelta() {
    provider.write("shadow.pb", shadowRegistry.getDelta().toByteString());
  }

  public void readDefault() {
    try {
      this.repo = new ProtoDescriptor(provider.read("default.pb").toByteArray());
    } catch (IOException e) {
      throw new RuntimeException("failed to read default.pb", e);
    }
  }

  public void readShadowDelta() {
    try {
      shadowRegistry.setDelta(Report.parseFrom(provider.read("shadow.pb")));
    } catch (IOException e) {
      throw new RuntimeException("failed to read shadow.pb", e);
    }
  }
}
