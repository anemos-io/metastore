package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.Patch;
import io.anemos.metastore.v1alpha1.Report;

public class ProfileProtoEvolve implements ValidationProfile {

  public String profileName = "proto:default";

  @Override
  public Report validate(Patch patch) {
    return null;
  }
}
