package io.anemos.metastore.core.proto.profile;

import static io.anemos.metastore.v1alpha1.FieldChangeInfo.FieldChangeType.*;
import static io.anemos.metastore.v1alpha1.FieldChangeInfo.FieldType.FIELD_TYPE_UNSET;

import io.anemos.metastore.v1alpha1.*;

public class ProfileAvroEvolve implements ValidationProfile {

  public String profileName = "proto:avro";

  @Override
  public Report validate(Report report) {
    ResultCount.Builder resultCountBuilder = ResultCount.newBuilder();

    Report.Builder builder = Report.newBuilder(report);
    int error = 0;
    for (MessageResult messageResult : builder.getMessageResultsMap().values()) {
      for (FieldResult fieldResult : messageResult.getFieldResultsList()) {
        if (fieldResult.getChange().getChangeType() == FIELD_REMOVED) {
          error++;
          resultCountBuilder.putErrorInfo(
              "CAVR-0001",
              String.format("Removal of fields is not allowed for profile '%s'.", profileName));
        } else if (fieldResult.getChange().getChangeType() == FIELD_RESERVED) {
          error++;
          resultCountBuilder.putErrorInfo(
              "CAVR-0002",
              String.format("Reservation of fields is not allowed for profile '%s'.", profileName));
        } else if (fieldResult.getChange().getChangeType() == FIELD_CHANGED) {
          if (isDeprecationOnly(fieldResult.getChange())) {
            // this is allowed
            continue;
          }

          error++;
          resultCountBuilder.putErrorInfo(
              "CAVR-0003",
              String.format("Changing fields is not allowed for profile '%s'.", profileName));
        }
      }
    }
    builder.setResultCount(resultCountBuilder.setDiffErrors(error).build());
    return builder.build();
  }

  private boolean isDeprecationOnly(FieldChangeInfo info) {
    return info.getFromDeprecated() == false
        && info.getToDeprecated() == true
        && info.getFromName().equals("")
        && info.getToName().equals("")
        && info.getFromType() == FIELD_TYPE_UNSET
        && info.getToType() == FIELD_TYPE_UNSET;
  }
}
