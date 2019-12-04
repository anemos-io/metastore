package io.anemos.metastore.core.proto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.UnsignedInteger;
import com.google.protobuf.Descriptors;
import io.anemos.metastore.putils.ProtoDomain;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class generate a Json schema from protobuf object https://json-schema.org or
 * https://cswr.github.io/JsonSchema/
 */
public class ProtoToJsonSchema {
  private Map<String, ObjectNode> enumMaps;
  private ObjectNode nestedNodes;
  private Map<String, ArrayNode> mapFieldsRequired;

  public ProtoToJsonSchema() {
    enumMaps = new HashMap<>();
    mapFieldsRequired = new HashMap<>();
  }

  /**
   * Convert Protobuf object to json schema
   *
   * @param pContainer
   * @param messageName
   * @return
   * @throws StatusRuntimeException
   */
  public static String convert(ProtoDomain pContainer, String messageName)
      throws StatusRuntimeException {

    Descriptors.Descriptor descriptor = pContainer.getDescriptorByName(messageName);
    if (descriptor == null)
      throw Status.fromCode(Status.Code.NOT_FOUND)
          .withDescription(String.format("The contract : %s is not found.", messageName))
          .asRuntimeException();
    return new ProtoToJsonSchema().toJsonSchema(descriptor).toString();
  }

  private ObjectNode toJsonSchema(Descriptors.Descriptor descriptor) {
    ObjectMapper mapper = new ObjectMapper();

    nestedNodes = mapper.createObjectNode();
    final ObjectNode node = mapper.createObjectNode();
    final ObjectNode nodeFields = toRecord(mapper, descriptor, null);

    if (nestedNodes.size() > 0) {
      // node.put("$schema", "http://json-schema.org/draft-07/schema#");
      node.put("definitions", nestedNodes);
    }
    node.put("title", descriptor.getFullName());
    node.put("type", "object");
    node.put("properties", nodeFields);
    if (mapFieldsRequired.containsKey(descriptor.getFullName()))
      node.put("required", mapFieldsRequired.get(descriptor.getFullName()));
    return node;
  }

  private ObjectNode toRecord(
      ObjectMapper mapper, Descriptors.Descriptor descriptor, String fieldName) {

    ObjectNode nodeFields = mapper.createObjectNode();
    ArrayNode fieldsRequired = mapper.createArrayNode();

    descriptor
        .getFields()
        .forEach(
            f -> {
              Descriptors.FieldDescriptor.Type descriptorType = f.getType();
              String fullName = f.getFullName();
              ProtoJsonType protoJsonType = ProtoJsonType.MESSAGE;

              if (isFieldRequired(descriptorType)) fieldsRequired.add(f.getName());
              if (f.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
                final Descriptors.Descriptor message = f.getMessageType();
                descriptorType = getFieldDescriptorFromWrapper(message);

                protoJsonType = getProtoJsonType(message);

                if (descriptorType == Descriptors.FieldDescriptor.Type.ENUM) {
                  addToEnumsMap(mapper, fullName, message.getEnumTypes());
                } else if (descriptorType == Descriptors.FieldDescriptor.Type.MESSAGE) {
                  final ObjectNode nodeNested = toRecord(mapper, message, f.getFullName());
                  fullName = message.getFullName();
                  addNestedNode(fullName, nodeNested);
                }
              } else if (f.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
                addToEnumsMap(mapper, f.getFullName(), f.getEnumType());
              }

              ObjectNode nodeFieldType =
                  toJsonNode(
                      fullName, descriptorType, mapper, isRepeatedDescriptor(f), protoJsonType);

              nodeFields.put(f.getName(), nodeFieldType);
            });

    if (fieldsRequired.size() > 0) mapFieldsRequired.put(descriptor.getFullName(), fieldsRequired);
    return nodeFields;
  }

  private ObjectNode toJsonNode(
      String fullName,
      Descriptors.FieldDescriptor.Type fieldType,
      ObjectMapper mapper,
      Boolean isRepeated,
      ProtoJsonType protoJsonType) {
    final ObjectNode nodeFieldItem = mapper.createObjectNode();
    final ObjectNode nodeFieldType = mapper.createObjectNode();

    if (fieldType == Descriptors.FieldDescriptor.Type.MESSAGE)
      nodeFieldType.put("$ref", "#" + fullName);
    else {
      String jsonType = getJsonType(fieldType);
      nodeFieldType.put("type", jsonType);

      String fieldPattern = getFieldPattern(protoJsonType);
      if (fieldType == Descriptors.FieldDescriptor.Type.ENUM)
        nodeFieldType.putAll(enumMaps.get(fullName));
      else if (fieldPattern != null) nodeFieldType.put("pattern", fieldPattern);
    }

    if (isRepeated) {
      nodeFieldItem.put("type", "array");
      nodeFieldItem.put("items", nodeFieldType);
    } else nodeFieldItem.putAll(nodeFieldType);

    final ObjectNode nodeRestrictive = mapper.createObjectNode();
    getRestrictives(fieldType, nodeRestrictive);
    nodeFieldItem.putAll(nodeRestrictive);

    return nodeFieldItem;
  }

  private String getJsonType(Descriptors.FieldDescriptor.Type fieldType) {
    switch (fieldType) {
      case SFIXED32:
      case INT32:
      case SINT32:
      case FIXED32:
      case UINT32:
      case SFIXED64:
      case INT64:
      case SINT64:
      case FIXED64:
      case UINT64:
        return "integer";
      case FLOAT:
      case DOUBLE:
        return "number";
      case BYTES:
      case ENUM:
      case STRING:
      case MESSAGE:
        return "string";
      case BOOL:
        return "boolean";
      default:
        throw Status.fromCode(Status.Code.UNIMPLEMENTED)
            .withDescription(fieldType.toString() + " is not implemented yet.")
            .asRuntimeException();
    }
  }

  private Boolean isFieldRequired(Descriptors.FieldDescriptor.Type fieldType) {
    switch (fieldType) {
      case SFIXED32:
      case INT32:
      case SINT32:
      case FIXED32:
      case UINT32:
      case SFIXED64:
      case INT64:
      case SINT64:
      case FIXED64:
      case UINT64:
      case FLOAT:
      case DOUBLE:
      case BYTES:
      case STRING:
      case BOOL:
        return true;
      default:
        return false;
    }
  }

  private void getRestrictives(Descriptors.FieldDescriptor.Type fieldType, ObjectNode node) {
    switch (fieldType) {
      case SFIXED32:
      case INT32:
      case SINT32:
        node.put("minimum", Integer.MIN_VALUE);
        node.put("maximum", Integer.MAX_VALUE);
        break;
      case SFIXED64:
      case INT64:
      case SINT64:
        node.put("minimum", Long.MIN_VALUE);
        node.put("maximum", Long.MAX_VALUE);
        break;
      case FIXED32:
      case UINT32:
        node.put("minimum", 0);
        node.put("maximum", UnsignedInteger.MAX_VALUE.longValue());
        break;
      case FIXED64:
      case UINT64:
        node.put("minimum", 0);
        node.put("maximum", Long.parseUnsignedLong("123456789012345678"));
        break;
      case FLOAT:
        node.put("minimum", Float.MIN_VALUE);
        node.put("maximum", Float.MIN_VALUE);
        break;
      case DOUBLE:
        node.put("minimum", Double.MIN_VALUE);
        node.put("maximum", Double.MIN_VALUE);
        break;
      case BYTES:
        node.put("contentEncoding", "base64");
        break;
    }
  }

  private Descriptors.FieldDescriptor.Type getFieldDescriptorFromWrapper(
      Descriptors.Descriptor descriptor) {
    switch (descriptor.getFullName()) {
      case "google.protobuf.Int32Value":
        return Descriptors.FieldDescriptor.Type.INT32;
      case "google.protobuf.UInt32Value":
        return Descriptors.FieldDescriptor.Type.UINT32;
      case "google.protobuf.Int64Value":
        return Descriptors.FieldDescriptor.Type.INT64;
      case "google.protobuf.UInt64Value":
        return Descriptors.FieldDescriptor.Type.UINT64;
      case "google.protobuf.FloatValue":
        return Descriptors.FieldDescriptor.Type.FLOAT;
      case "google.protobuf.DoubleValue":
        return Descriptors.FieldDescriptor.Type.DOUBLE;
      case "google.protobuf.BytesValue":
        return Descriptors.FieldDescriptor.Type.BYTES;
      case "google.protobuf.Duration":
      case "google.protobuf.Timestamp":
      case "google.protobuf.StringValue":
        return Descriptors.FieldDescriptor.Type.STRING;
      case "google.protobuf.BoolValue":
        return Descriptors.FieldDescriptor.Type.BOOL;
      default:
        if (descriptor.getEnumTypes().size() > 0) return Descriptors.FieldDescriptor.Type.ENUM;
        else if (descriptor.getFields().size() > 0) return Descriptors.FieldDescriptor.Type.MESSAGE;
        else return null;
    }
  }

  private ProtoJsonType getProtoJsonType(Descriptors.Descriptor descriptor) {
    switch (descriptor.getFullName()) {
      case "google.protobuf.Duration":
        return ProtoJsonType.DURATION;
      case "google.protobuf.Timestamp":
        return ProtoJsonType.TIMESTAMP;
      case "google.protobuf.EnumValue":
        return ProtoJsonType.ENUM;
      default:
        return ProtoJsonType.MESSAGE;
    }
  }

  private void addToEnumsMap(
      ObjectMapper mapper, String fieldName, List<Descriptors.EnumDescriptor> enums) {
    enums.forEach(
        item -> {
          addToEnumsMap(mapper, fieldName, item);
        });
  }

  private void addToEnumsMap(
      ObjectMapper mapper, String fieldName, Descriptors.EnumDescriptor item) {
    ObjectNode nodeEnum = mapper.createObjectNode();
    ArrayNode nodeArray = nodeEnum.putArray("enum");
    item.getValues()
        .forEach(
            e -> {
              nodeArray.add(e.getName());
            });
    String key = fieldName == null ? item.getFullName() : fieldName;
    enumMaps.put(key, nodeEnum);
  }

  private void addNestedNode(String fullName, ObjectNode nodeNested) {
    ObjectNode objObject = nestedNodes.putObject(fullName);
    objObject.put("$id", "#" + fullName);
    objObject.put("type", "object");
    objObject.put("properties", nodeNested);

    if (mapFieldsRequired.containsKey(fullName))
      objObject.put("required", mapFieldsRequired.get(fullName));
  }

  private Boolean isRepeatedDescriptor(Descriptors.FieldDescriptor field) {
    return field.toProto().getLabel().name().equals("LABEL_REPEATED");
  }

  private String getFieldPattern(ProtoJsonType type) {
    switch (type) {
      case TIMESTAMP:
        return "^[0-9]{4}[-](0[1-9]|1[012])[-](0[1-9]|[12][0-9]|3[01])[T]([01][1-9]|[2][0-3])[:]([0-5][0-9])[:]([0-5][0-9])([.][0-9]{0,9}){0,1}[Z]$";
      case DURATION:
        return "^[0-9]+([.][0-9]{0,9}){0,1}[s]$";
      default:
        return null;
    }
  }

  private enum ProtoJsonType {
    TIMESTAMP,
    ENUM,
    DURATION,
    MESSAGE
  }
}
