package io.anemos.metastore.core.proto.profile;

import io.anemos.metastore.v1alpha1.*;
import java.util.regex.Pattern;

public abstract class ProfileAllowAddBase implements ValidationProfile {

  public final String profileName;

  private boolean skipValidationForAlpha;
  Pattern alphaPattern = Pattern.compile(".*\\.v\\d*alpha\\d*$");

  public ProfileAllowAddBase(String profileName, boolean skipValidationForAlpha) {
    this.profileName = profileName;
    this.skipValidationForAlpha = skipValidationForAlpha;
  }

  public String getProfileName() {
    return profileName;
  }

  private boolean skipValidationForAlpha(String packageName) {
    return skipValidationForAlpha == true && alphaPattern.matcher(packageName).matches();
  }

  @Override
  public ValidationSummary validate(Patch patch) {
    ValidationSummary.Builder builder = ValidationSummary.newBuilder();

    int error = 0;
    for (MessagePatch messageResult : patch.getMessagePatchesMap().values()) {
      if (skipValidationForAlpha(messageResult.getPackage())) {
        continue;
      }
      switch (messageResult.getNameChange().getChangeType()) {
        case REMOVAL:
          error++;
          builder.addErrorInfo(
              ErrorInfo.newBuilder()
                  .setType(ErrorType.ERROR)
                  .setMessage(messageResult.getName())
                  .setCode("CAVR-0001")
                  .setDescription(
                      String.format(
                          "Removal of messages is not allowed for profile '%s'.", profileName)));
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
            builder.addErrorInfo(
                ErrorInfo.newBuilder()
                    .setType(ErrorType.ERROR)
                    .setField(messageResult.getName() + "#" + fieldResult.getName())
                    .setCode("CAVR-0001")
                    .setDescription(
                        String.format(
                            "Removal of fields is not allowed for profile '%s'.", profileName)));
            break;
          case RESERVED:
            error++;
            builder.addErrorInfo(
                ErrorInfo.newBuilder()
                    .setType(ErrorType.ERROR)
                    .setField(messageResult.getName() + "#" + fieldResult.getName())
                    .setCode("CAVR-0002")
                    .setDescription(
                        String.format(
                            "Reservation of fields is not allowed for profile '%s'.",
                            profileName)));
            break;
          case CHANGED:
            error++;
            builder.addErrorInfo(
                ErrorInfo.newBuilder()
                    .setType(ErrorType.ERROR)
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
    for (EnumPatch enumResult : patch.getEnumPatchesMap().values()) {
      if (skipValidationForAlpha(enumResult.getPackage())) {
        continue;
      }
      switch (enumResult.getNameChange().getChangeType()) {
        case REMOVAL:
          error++;
          builder.addErrorInfo(
              ErrorInfo.newBuilder()
                  .setType(ErrorType.ERROR)
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
      for (EnumValuePatch valueResult : enumResult.getValuePatchesList()) {
        switch (valueResult.getValueChange().getChangeType()) {
          case REMOVAL:
            error++;
            builder.addErrorInfo(
                ErrorInfo.newBuilder()
                    .setType(ErrorType.ERROR)
                    .setField(enumResult.getName() + "#" + valueResult.getName())
                    .setCode("CAVR-0001")
                    .setDescription(
                        String.format(
                            "Removal of enum values is not allowed for profile '%s'.",
                            profileName)));
            break;
          case RESERVED:
            error++;
            builder.addErrorInfo(
                ErrorInfo.newBuilder()
                    .setType(ErrorType.ERROR)
                    .setField(enumResult.getName() + "#" + valueResult.getName())
                    .setCode("CAVR-0002")
                    .setDescription(
                        String.format(
                            "Reservation of enum values is not allowed for profile '%s'.",
                            profileName)));
            break;
          case CHANGED:
            error++;
            builder.addErrorInfo(
                ErrorInfo.newBuilder()
                    .setType(ErrorType.ERROR)
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
    builder.setDiffErrors(error);
    return builder.build();
  }
}
