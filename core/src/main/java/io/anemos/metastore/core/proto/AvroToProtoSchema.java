package io.anemos.metastore.core.proto;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.CaseFormat;
import com.google.protobuf.*;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;

public class AvroToProtoSchema {

  private JsonNode node;

  public AvroToProtoSchema(JsonNode node) {
    this.node = node;
  }

  public FileDescriptorProto toDescriptor() {
    String recordName = node.path("name").asText();
    String recordNamespace = node.path("namespace").asText();
    // String recordType = node.path("type").asText();
    JsonNode fields = node.withArray("fields");

    FileDescriptorProto.Builder fileDescriptorProtoBuilder =
        FileDescriptorProto.newBuilder()
            .setName(recordName)
            .setPackage(recordNamespace)
            .setSyntax("proto3");

    DescriptorProtos.DescriptorProto.Builder descriptor =
        DescriptorProtos.DescriptorProto.newBuilder().setName(recordName);

    descriptor = getFields(descriptor, fields);

    fileDescriptorProtoBuilder.addMessageType(descriptor.build());

    return fileDescriptorProtoBuilder.build();
  }

  private DescriptorProtos.FieldDescriptorProto.Type toFieldDescriptorType(String jsonType) {
    switch (jsonType) {
      case "int":
        return DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32;
      case "string":
        return DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;
      case "boolean":
        return DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL;
      case "long":
        return DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64;
      case "float":
        return DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT;
      case "double":
        return DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE;
      case "bytes":
        return DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES;
      default:
        break;
    }
    return null;
  }

  private DescriptorProtos.DescriptorProto.Builder getFields(
      DescriptorProtos.DescriptorProto.Builder descriptor, JsonNode fields) {
    int i = 1;
    for (JsonNode field : fields) {
      String fieldName = field.path("name").asText();
      DescriptorProtos.FieldDescriptorProto.Builder fieldDescriptorProtoBuilder =
          DescriptorProtos.FieldDescriptorProto.newBuilder()
              .setName(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName))
              .setNumber(i);

      // CASO ARRAY
      if (field.path("type").asText().equals("array")) {
        // fieldType = field.path("type").asText();
        fieldDescriptorProtoBuilder.setType(toFieldDescriptorType(field.path("items").asText()));
        fieldDescriptorProtoBuilder.setLabel(
            DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED);
        // CASO ENUM
      } else if (field.path("type").path("type").asText().equals("enum")) {
        DescriptorProtos.EnumDescriptorProto.Builder enumDescriptorProto =
            DescriptorProtos.EnumDescriptorProto.newBuilder()
                .setName(field.path("type").path("name").asText());

        JsonNode symbols = field.path("type").withArray("symbols");
        int j = 0;
        for (JsonNode symbol : symbols) {
          DescriptorProtos.EnumValueDescriptorProto.Builder enumValueDescriptorProto =
              DescriptorProtos.EnumValueDescriptorProto.newBuilder();
          enumValueDescriptorProto.setName(symbol.asText()).setNumber(j).build();
          enumDescriptorProto.addValue(enumValueDescriptorProto).build();
          j++;
        }
        fieldDescriptorProtoBuilder.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM);
        fieldDescriptorProtoBuilder.setLabel(
            DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL);
        fieldDescriptorProtoBuilder.setTypeName(
            this.node
                .path("namespace")
                .asText()
                .concat(".")
                .concat(this.node.path("name").asText())
                .concat(".")
                .concat(field.path("type").path("name").asText()));
        descriptor.addEnumType(enumDescriptorProto).build();

        // TODO: add map complex type
      } else if (field.path("type").path("type").asText().equals("map")) {

      } else {
        String fieldType = field.path("type").asText();
        fieldDescriptorProtoBuilder.setType(toFieldDescriptorType(fieldType));
      }

      descriptor.addField(fieldDescriptorProtoBuilder);
      i++;
    }
    return descriptor;
  }

  private Boolean isSingleType(String type) {
    if (type.equals("array")) {
      return false;
    }
    return true;
  }
}
