package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.Patch;
import io.anemos.metastore.v1alpha1.ValidationSummary;

public class ProfileAllowAll implements ValidationProfile {

  public String profileName = "proto:allow";

  @Override
  public ValidationSummary validate(Patch patch) {
    return ValidationSummary.newBuilder().setValidationProfile(profileName).build();
  }
}
