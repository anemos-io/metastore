package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.Report;

public interface ValidationProfile {

  Report validate(Report report);
}
