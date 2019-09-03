package io.anemos.metastore.core.proto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.UnsignedInteger;
import com.google.protobuf.Descriptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** https://json-schema.org or https://cswr.github.io/JsonSchema/ */
public class ProtoToJsonSchema {
  private PContainer pContainer;
  private Map<String, ObjectNode> enumMaps;

  public ProtoToJsonSchema(PContainer pC) {
    this.pContainer = pC;
    enumMaps = new HashMap<>();
  }

  public static String convert(PContainer pContainer, String messageName)
      throws StatusRuntimeException {
    // Descriptors.Descriptor descriptor = pContainer.getDescriptorByName(messageName);
    return new ProtoToJsonSchema(pContainer).toJsonSchema(messageName).toString();
  }

  private ObjectNode toJsonSchema(String messageName) {
    Descriptors.Descriptor descriptor = pContainer.getDescriptorByName(messageName);
    if (descriptor == null)
      throw Status.fromCode(Status.Code.NOT_FOUND)
          .withDescription(String.format("The contract : %s is not found.", messageName))
          .asRuntimeException();

    ObjectMapper mapper = new ObjectMapper();

    final ObjectNode node = mapper.createObjectNode();
    node.put("title", descriptor.getFullName());
    node.put("type", "object");

    final ObjectNode nodeFields = toRecord(mapper, descriptor, null);

    node.put("properties", nodeFields);
    return node;
  }

  private ObjectNode toRecord(
      ObjectMapper mapper, Descriptors.Descriptor descriptor, String fieldName) {

    ObjectNode nodeFields = mapper.createObjectNode();

    descriptor
        .getFields()
        .forEach(
            f -> {
              Descriptors.FieldDescriptor.Type descriptorType = f.getType();

              String fullName = f.getFullName();

              if (f.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
                final Descriptors.Descriptor message = f.getMessageType();
                descriptorType = getMessageType(message);
                if (descriptorType == Descriptors.FieldDescriptor.Type.ENUM) {
                  getMapEnums(mapper, fullName, message.getEnumTypes());
                }

                // final ObjectNode nodeNested = toRecord(mapper, message, f.getFullName());
                // nodeFields.put(f.getName(), nodeNested);
              } else if (f.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
                getMapEnums(mapper, f.getFullName(), f.getEnumType());
              }

              final ObjectNode nodeFieldType = toJsonNode(fullName, descriptorType, mapper);
              nodeFields.put(f.getName(), nodeFieldType);
            });

    return nodeFields;
  }

  private ObjectNode toJsonNode(
      String fullName, Descriptors.FieldDescriptor.Type fieldType, ObjectMapper mapper) {
    final ObjectNode nodeFieldType = mapper.createObjectNode();
    String jsonType = getJsonType(fieldType);
    nodeFieldType.put("type", jsonType);

    if (fieldType == Descriptors.FieldDescriptor.Type.ENUM) {
      nodeFieldType.putAll(enumMaps.get(fullName));
    }

    final ObjectNode nodeRestrictive = mapper.createObjectNode();
    getRestrictives(fieldType, nodeRestrictive);
    nodeFieldType.putAll(nodeRestrictive);

    return nodeFieldType;
  }

  private String getJsonType(Descriptors.FieldDescriptor.Type fieldType) {
    switch (fieldType) {
      case SFIXED32:
      case INT32:
      case SINT32:
      case FIXED32:
      case UINT32:
        return "integer";
      case SFIXED64:
      case INT64:
      case SINT64:
      case FIXED64:
      case UINT64:
      case FLOAT:
      case DOUBLE:
        return "numeric";
      case BYTES:
      case ENUM:
      case STRING:
        return "string";
      case BOOL:
        return "boolean";
      default:
        throw Status.fromCode(Status.Code.UNIMPLEMENTED)
            .withDescription(fieldType.toString() + " is not implemented yet.")
            .asRuntimeException();
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

  private void getMapEnums(
      ObjectMapper mapper, String fieldName, List<Descriptors.EnumDescriptor> enums) {
    enums.forEach(
        item -> {
          getMapEnums(mapper, fieldName, item);
        });
  }

  private void getMapEnums(ObjectMapper mapper, String fieldName, Descriptors.EnumDescriptor item) {
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

  private Descriptors.FieldDescriptor.Type getMessageType(Descriptors.Descriptor message) {
    if (message.getEnumTypes() != null) return Descriptors.FieldDescriptor.Type.ENUM;

    return null;
  }
}
