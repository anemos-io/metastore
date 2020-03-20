package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.*;

public class ProfileAllowStableAddAlphaAll extends ProfileAllowAddBase {
  public ProfileAllowStableAddAlphaAll() {
    super("allow:stable:add:alpha:all");
  }

  @Override
  public Report validate(Report report) {
    final String v1alpha = "v1alpha";
    Report.Builder builder = Report.newBuilder(report);

    for (MessageResult messageResult : report.getMessageResultsMap().values()) {
      if (messageResult.getName().contains(v1alpha)) {
        builder.removeMessageResults(messageResult.getName());
      }
    }

    for (EnumResult enumResult : report.getEnumResultsMap().values()) {
      if (enumResult.getName().contains(v1alpha)) {
        builder.removeEnumResults(enumResult.getName());
      }
    }

    return super.validate(builder.build());
  }
}