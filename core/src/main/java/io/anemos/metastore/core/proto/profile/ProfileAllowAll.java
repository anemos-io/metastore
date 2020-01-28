package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.Patch;
import io.anemos.metastore.v1alpha1.Report;

public class ProfileAllowAll implements ValidationProfile {

  public String profileName = "proto:allow";

  @Override
  public Report validate(Patch patch) {
    return Report.newBuilder().setPatch(patch).build();
  }
}
