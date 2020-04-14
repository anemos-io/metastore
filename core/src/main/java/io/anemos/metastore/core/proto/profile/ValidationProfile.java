package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.Patch;
import io.anemos.metastore.v1alpha1.ValidationSummary;

public interface ValidationProfile {

  ValidationSummary validate(Patch patch);
}
