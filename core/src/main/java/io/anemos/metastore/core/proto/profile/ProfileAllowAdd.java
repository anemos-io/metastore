package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.*;

public class ProfileAllowAdd implements ValidationProfile {

  public String profileName = "allow:add";

  @Override
  public Report validate(Patch patch) {
    ResultCount.Builder resultCountBuilder = ResultCount.newBuilder();
    Patch.Builder patchBuilder = Patch.newBuilder(patch);

    int error = 0;
    for (MessagePatch messageResult : patchBuilder.getMessagePatchesMap().values()) {
      switch (messageResult.getChange().getChangeType()) {
        case REMOVAL:
          error++;
          resultCountBuilder.addErrorInfo(
              ErrorInfo.newBuilder()
                  .setType(ErrorInfo.ErrorType.ERROR)
                  .setMessage(messageResult.getName())
                  .setCode("CAVR-0001")
                  .setDescription(
                      String.format(
                          "Removal of fields is not allowed for profile '%s'.", profileName)));
          break;
        case RESERVED:
        case UNRESERVED:
        case UNCHANGED:
        case CHANGED:
        case ADDITION:
        case DEPRECATED:
        case REINSTATED:
        case PAYLOAD_CHANGED:
        case UNRECOGNIZED:
          break;
      }
      for (FieldPatch fieldResult : messageResult.getFieldPatchesList()) {
        switch (fieldResult.getChange().getChangeType()) {
          case REMOVAL:
            error++;
            resultCountBuilder.addErrorInfo(
                ErrorInfo.newBuilder()
                    .setType(ErrorInfo.ErrorType.ERROR)
                    .setField(messageResult.getName() + "#" + fieldResult.getName())
                    .setCode("CAVR-0001")
                    .setDescription(
                        String.format(
                            "Removal of fields is not allowed for profile '%s'.", profileName)));
            break;
          case RESERVED:
            error++;
            resultCountBuilder.addErrorInfo(
                ErrorInfo.newBuilder()
                    .setType(ErrorInfo.ErrorType.ERROR)
                    .setField(messageResult.getName() + "#" + fieldResult.getName())
                    .setCode("CAVR-0002")
                    .setDescription(
                        String.format(
                            "Reservation of fields is not allowed for profile '%s'.",
                            profileName)));
            break;
          case CHANGED:
            error++;
            resultCountBuilder.addErrorInfo(
                ErrorInfo.newBuilder()
                    .setType(ErrorInfo.ErrorType.ERROR)
                    .setField(messageResult.getName() + "#" + fieldResult.getName())
                    .setCode("CAVR-0003")
                    .setDescription(
                        String.format(
                            "Changing fields is not allowed for profile '%s'.", profileName)));
            break;
          case UNRESERVED:
          case UNCHANGED:
          case ADDITION:
          case DEPRECATED:
          case REINSTATED:
          case PAYLOAD_CHANGED:
          case UNRECOGNIZED:
            break;
        }
      }
    }
    for (EnumPatch enumResult : patchBuilder.getEnumPatchesMap().values()) {
      switch (enumResult.getChange().getChangeType()) {
        case REMOVAL:
          error++;
          resultCountBuilder.addErrorInfo(
              ErrorInfo.newBuilder()
                  .setType(ErrorInfo.ErrorType.ERROR)
                  .setEnum(enumResult.getName())
                  .setCode("CAVR-0001")
                  .setDescription(
                      String.format(
                          "Removal of enums is not allowed for profile '%s'.", profileName)));
          break;
        case RESERVED:
        case UNRESERVED:
        case UNCHANGED:
        case CHANGED:
        case ADDITION:
        case DEPRECATED:
        case REINSTATED:
        case PAYLOAD_CHANGED:
        case UNRECOGNIZED:
          break;
      }
      for (EnumValuePatch valueResult : enumResult.getValueResultsList()) {
        switch (valueResult.getChange().getChangeType()) {
          case REMOVAL:
            error++;
            resultCountBuilder.addErrorInfo(
                ErrorInfo.newBuilder()
                    .setType(ErrorInfo.ErrorType.ERROR)
                    .setField(enumResult.getName() + "#" + valueResult.getName())
                    .setCode("CAVR-0001")
                    .setDescription(
                        String.format(
                            "Removal of enum values is not allowed for profile '%s'.",
                            profileName)));
            break;
          case RESERVED:
            error++;
            resultCountBuilder.addErrorInfo(
                ErrorInfo.newBuilder()
                    .setType(ErrorInfo.ErrorType.ERROR)
                    .setField(enumResult.getName() + "#" + valueResult.getName())
                    .setCode("CAVR-0002")
                    .setDescription(
                        String.format(
                            "Reservation of enum values is not allowed for profile '%s'.",
                            profileName)));
            break;
          case CHANGED:
            error++;
            resultCountBuilder.addErrorInfo(
                ErrorInfo.newBuilder()
                    .setType(ErrorInfo.ErrorType.ERROR)
                    .setField(enumResult.getName() + "#" + valueResult.getName())
                    .setCode("CAVR-0003")
                    .setDescription(
                        String.format(
                            "Changing enum values is not allowed for profile '%s'.", profileName)));
            break;
          case UNRESERVED:
          case UNCHANGED:
          case ADDITION:
          case DEPRECATED:
          case REINSTATED:
          case PAYLOAD_CHANGED:
          case UNRECOGNIZED:
            break;
        }
      }
    }
    Report.Builder reportBuilder = Report.newBuilder();
    reportBuilder.setPatch(patchBuilder);
    reportBuilder.setResultCount(resultCountBuilder.setDiffErrors(error).build());
    return reportBuilder.build();
  }
}
