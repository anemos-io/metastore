package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.Patch;
import io.anemos.metastore.v1alpha1.Report;

public class ProfileAllowNone implements ValidationProfile {

  public String profileName = "proto:none";

  // TODO implement
  @Override
  public Report validate(Patch patch) {
    return Report.newBuilder().setPatch(patch).build();
  }
}
