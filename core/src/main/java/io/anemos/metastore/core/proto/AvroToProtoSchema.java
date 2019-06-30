package io.anemos.metastore.core.proto;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.CaseFormat;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Int64Value;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AvroToProtoSchema {

  private static List<String> avroPrimitives = new ArrayList<>();
  private static List<String> avroReserved = new ArrayList<>();

  private boolean pragmaUseWrappers = true;

  static {
    avroPrimitives.add("boolean");
    avroPrimitives.add("int");
    avroPrimitives.add("long");
    avroPrimitives.add("float");
    avroPrimitives.add("double");
    avroPrimitives.add("bytes");
    avroPrimitives.add("string");
    avroReserved.addAll(avroPrimitives);
    avroReserved.add("null");
    avroReserved.add("record");
    avroReserved.add("enum");
    avroReserved.add("array");
    avroReserved.add("map");
    avroReserved.add("fixed");
  }

  private Map<AvroRecordName, JsonNode> complexAvroMap = new HashMap<>();

  private JsonNode root;
  private String rootNamespace;

  private Map<String, Set<String>> importMap = new HashMap<>();
  private Map<String, DescriptorProtos.DescriptorProto> messageMap = new HashMap<>();
  private Map<String, DescriptorProtos.EnumDescriptorProto> enumMap = new HashMap<>();

  public AvroToProtoSchema(JsonNode node) {
    this.root = node;
  }

  public PContainer get() {
    rootNamespace = root.path("namespace").asText();

    protoMessageOf(root);

    Map<String, FileDescriptorProto.Builder> fileMap = new HashMap<>();

    messageMap.forEach(
        (fullName, message) -> {
          String packageName =
              fullName.substring(0, fullName.length() - message.getName().length() - 1);
          FileDescriptorProto.Builder fdp = fileMap.get(packageName);
          if (fdp == null) {
            fdp =
                DescriptorProtos.FileDescriptorProto.newBuilder()
                    .setName(packageNameToFileName(packageName))
                    .setPackage(packageName)
                    .setSyntax("proto3");
            fileMap.put(packageName, fdp);
          }
          fdp.addMessageType(message);
        });

    DescriptorProtos.FileDescriptorSet.Builder fds =
        DescriptorProtos.FileDescriptorSet.newBuilder();
    fileMap.forEach(
        (name, fdp) -> {
          Set<String> imports = importMap.get(fdp.getPackage());
          if (imports != null) {
            imports.forEach(im -> fdp.addDependency(im));
          }

          fds.addFile(fdp);
        });
    fds.addFile(Int64Value.getDescriptor().getFile().toProto());

    return new PContainer(fds.build());
  }

  private DescriptorProtos.EnumDescriptorProto.Builder protoEnumOf(
      JsonNode enumNode, boolean nested) {
    DescriptorProtos.EnumDescriptorProto.Builder enumDescriptorProto =
        DescriptorProtos.EnumDescriptorProto.newBuilder()
            .setName(enumNode.path("type").path("name").asText());

    JsonNode symbols = enumNode.path("type").withArray("symbols");
    int j = 0;
    for (JsonNode symbol : symbols) {
      DescriptorProtos.EnumValueDescriptorProto.Builder enumValueDescriptorProto =
          DescriptorProtos.EnumValueDescriptorProto.newBuilder();
      enumValueDescriptorProto.setName(symbol.asText()).setNumber(j).build();
      enumDescriptorProto.addValue(enumValueDescriptorProto).build();
      j++;
    }
    if (!nested) {
      enumMap.put(
          fullName(rootNamespace, enumDescriptorProto.getName()), enumDescriptorProto.build());
    }
    return enumDescriptorProto;
  }

  private DescriptorProtos.FieldDescriptorProto.Builder protoFieldOf(
      DescriptorProtos.DescriptorProto.Builder messageDescriptor,
      DescriptorProtos.FieldDescriptorProto.Builder fieldDescriptor,
      String packageName,
      JsonNode field) {
    if (isFieldSimple(field)) {
      fieldDescriptor.setType(extractPrimitiveType(field.path("type")));
    } else if (isFieldArray(field)) {
      // fieldType = field.path("type").asText();
      fieldDescriptor.setType(extractPrimitiveType(field.path("items")));
      fieldDescriptor.setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED);
    } else if (isFieldComplex(field)) {
      JsonNode typeNode = extractComplexTypeNode(field.get("type"));
      String nestedPackageName = extractPackageName(typeNode);
      DescriptorProtos.DescriptorProto.Builder messageProto = protoMessageOf(typeNode);
      fieldDescriptor.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE);
      fieldDescriptor.setTypeName(nestedPackageName.concat(".").concat(messageProto.getName()));
      addImportPackage(packageName, nestedPackageName);
    } else if (isFieldUnion(field)) {
      boolean isNull = isNullUnion(field);
      DescriptorProtos.FieldDescriptorProto.Type primitiveType =
          extractPrimitiveType(extractNoneNullInUnion(field));
      if (pragmaUseWrappers) {
        fieldDescriptor.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE);
        switch (primitiveType) {
          case TYPE_STRING:
            fieldDescriptor.setTypeName("google.protobuf.StringValue");
            break;
          case TYPE_INT32:
            fieldDescriptor.setTypeName("google.protobuf.Int32Value");
            break;
          case TYPE_INT64:
            fieldDescriptor.setTypeName("google.protobuf.Int64Value");
            break;
          case TYPE_BOOL:
            fieldDescriptor.setTypeName("google.protobuf.BoolValue");
            break;
          default:
            throw new RuntimeException("Unable to habdle " + primitiveType);
        }
        addImportFileName(packageName, "google/protobuf/wrappers.proto");
      } else {
        fieldDescriptor.setType(primitiveType);
      }
    } else if (field.path("type").path("type").asText().equals("enum")) {
      DescriptorProtos.EnumDescriptorProto.Builder enumProto = protoEnumOf(field, true);
      fieldDescriptor.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM);
      fieldDescriptor.setTypeName(
          packageName
              .concat(".")
              .concat(messageDescriptor.getName())
              .concat(".")
              .concat(enumProto.getName()));
      messageDescriptor.addEnumType(enumProto);
    } else if (field.path("type").path("type").asText().equals("map")) {
      throw new RuntimeException("Unhandler conversion (map) of field");

    } else {
      throw new RuntimeException("Unhandler conversion of field");
    }
    return fieldDescriptor;
  }

  private void addImportPackage(String packageName, String importedPackageName) {
    if (packageName.equals(importedPackageName)) {
      return;
    }
    addImportFileName(packageName, packageNameToFileName(importedPackageName));
  }

  private void addImportFileName(String packageName, String fileName) {
    Set<String> imports = importMap.get(packageName);
    if (imports == null) {
      imports = new HashSet<>();
      importMap.put(packageName, imports);
    }
    imports.add(fileName);
  }

  private boolean isNullUnion(JsonNode field) {
    for (JsonNode jsonNode : field.get("type")) {
      if (jsonNode.isTextual() && "null".equals(jsonNode.textValue())) {
        return true;
      }
    }
    return false;
  }

  private JsonNode extractNoneNullInUnion(JsonNode field) {
    for (JsonNode jsonNode : field.get("type")) {
      if (!jsonNode.isTextual() || (jsonNode.isTextual() && !"null".equals(jsonNode.textValue()))) {
        return jsonNode;
      }
    }
    return null;
  }

  private DescriptorProtos.DescriptorProto.Builder protoMessageOf(JsonNode complexNode) {
    String messageName = extractName(complexNode);
    String packageName = extractPackageName(complexNode);

    DescriptorProtos.DescriptorProto.Builder descriptor =
        DescriptorProtos.DescriptorProto.newBuilder().setName(messageName);

    JsonNode fields = complexNode.withArray("fields");

    int i = 1;
    for (JsonNode field : fields) {
      String fieldName = field.path("name").asText();
      DescriptorProtos.FieldDescriptorProto.Builder fieldDescriptor =
          DescriptorProtos.FieldDescriptorProto.newBuilder()
              .setName(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName))
              .setNumber(i);

      fieldDescriptor = protoFieldOf(descriptor, fieldDescriptor, packageName, field);
      descriptor.addField(fieldDescriptor);
      i++;
    }

    messageMap.put(fullName(packageName, messageName), descriptor.build());
    return descriptor;
  }

  private String fullName(String namespace, String name) {
    return namespace + "." + name;
  }

  private boolean isFieldSimple(JsonNode fieldNode) {
    return extractPrimitiveType(fieldNode.get("type")) != null;
  }

  private boolean isFieldUnion(JsonNode fieldNode) {
    return fieldNode.get("type").isArray();
  }

  private boolean isFieldArray(JsonNode fieldNode) {
    return fieldNode.get("type").isTextual() && "array".equals(fieldNode.get("type").asText());
  }

  private boolean isFieldComplex(JsonNode fieldNode) {
    return extractComplexTypeNode(fieldNode.get("type")) != null;
    //        JsonNode typeNode = fieldNode.get("type");
    //        if (typeNode.isObject()) {
    //            if ("record".equals(typeNode.get("type").textValue())) {
    //                return true;
    //            }
    //        } else if (typeNode.isArray()) {
    //            for (JsonNode subTypeNode : typeNode) {
    //                if (subTypeNode.isObject() &&
    // "record".equals(subTypeNode.get("type").textValue())) {
    //                    return true;
    //                } else if (subTypeNode.isTextual() &&
    // !avroReserved.contains(subTypeNode.asText())) {
    //                    return true;
    //                }
    //            }
    //        }
    //        return false;
  }

  private DescriptorProtos.FieldDescriptorProto.Type extractPrimitiveType(JsonNode typeNode) {
    if (typeNode.isTextual()) {
      String jsonType = typeNode.asText();
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
    } else if (typeNode.isObject()) {
      return extractPrimitiveType(typeNode.get("type"));
    }
    return null;
  }

  private JsonNode extractComplexTypeNode(JsonNode typeNode) {
    if (typeNode.isObject()) {
      if ("record".equals(typeNode.get("type").textValue())) {
        complexAvroMap.put(new AvroRecordName(typeNode), typeNode);
        return typeNode;
      }
    } else if (typeNode.isArray()) {
      for (JsonNode subTypeNode : typeNode) {
        if (subTypeNode.isObject() && "record".equals(subTypeNode.get("type").textValue())) {
          complexAvroMap.put(new AvroRecordName(subTypeNode), subTypeNode);
          return subTypeNode;
        } else if (subTypeNode.isTextual() && !avroReserved.contains(subTypeNode.asText())) {
          return complexAvroMap.get(new AvroRecordName(rootNamespace, subTypeNode.asText()));
        }
      }
    }
    return null;
  }

  private String extractPackageName(JsonNode typeNode) {
    String namespace;
    if (typeNode.has("namespace")) {
      namespace = typeNode.get("namespace").textValue();
    } else {
      namespace = rootNamespace;
    }
    return namespace;
  }

  private String extractName(JsonNode typeNode) {
    String name = typeNode.get("name").textValue();
    return name;
  }

  private Boolean isSingleType(String type) {
    if (type.equals("array")) {
      return false;
    }
    return true;
  }

  private String packageNameToFileName(String packageName) {
    return packageName.replace('.', '/') + ".proto";
  }

  private class AvroRecordName {
    String namespace;
    String name;

    public AvroRecordName(String namespace, String name) {
      this.namespace = namespace;
      this.name = name;
    }

    public AvroRecordName(String name) {
      this.name = name;
    }

    public AvroRecordName(JsonNode typeNode) {
      this.name = typeNode.get("name").textValue();
      if (typeNode.has("namespace")) {
        this.namespace = typeNode.get("namespace").textValue();
      } else {
        this.namespace = rootNamespace;
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      AvroRecordName that = (AvroRecordName) o;
      return Objects.equals(namespace, that.namespace) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(namespace, name);
    }
  }
}
