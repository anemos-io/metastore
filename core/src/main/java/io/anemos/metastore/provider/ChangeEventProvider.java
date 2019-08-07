package io.anemos.metastore.provider;

import io.anemos.metastore.v1alpha1.Report;
import java.util.Map;

/**
 * A ChangeEventProvider needs to implement this interface, it enables metastore to send
 * notifications about changes in contracts.
 */
public interface ChangeEventProvider {

  void initForChangeEvent(RegistryInfo registryInfo, Map<String, String> config);

  /** Send event for descriptor changes. */
  void descriptorsChanged(Report report);
}
