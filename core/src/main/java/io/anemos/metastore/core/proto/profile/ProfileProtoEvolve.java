package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.Patch;
import io.anemos.metastore.v1alpha1.ValidationSummary;

public class ProfileProtoEvolve implements ValidationProfile {

  public String profileName = "proto:default";

  @Override
  public ValidationSummary validate(Patch patch) {
    return ValidationSummary.newBuilder().setValidationProfile(profileName).build();
  }
}
