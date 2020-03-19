package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.Report;

public class ProfileAllowStableAddAlphaAll extends ProfileAllowAddBase {
  public ProfileAllowStableAddAlphaAll() {
    super("allow:stable:add:alpha:all");
  }

  @Override
  public Report validate(Report report) {
    final long v1alphaCount =
        report.getFileResultsMap().keySet().stream().filter(f -> f.contains("v1alpha")).count();

    if (v1alphaCount > 0) {
      return report;
    } else {
      return super.validate(report);
    }
  }
}
