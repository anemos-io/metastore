package io.anemos.metastore.core.proto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.UnsignedInteger;
import com.google.protobuf.Descriptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/** https://json-schema.org or https://cswr.github.io/JsonSchema/ */
public class ProtoToJsonSchema {

  public static String convert(PContainer pd, String messageName) throws StatusRuntimeException {
    Descriptors.Descriptor descriptor = pd.getDescriptorByName(messageName);
    if (descriptor == null)
      throw Status.fromCode(Status.Code.NOT_FOUND)
          .withDescription(String.format("The contract : %s is not found.", messageName))
          .asRuntimeException();
    return new ProtoToJsonSchema().toRecord(descriptor).toString();
  }

  private String toJsonType(Descriptors.FieldDescriptor.Type fieldDescriptor) {
    switch (fieldDescriptor) {
      case INT32:
      case UINT32:
      case SINT32:
      case FIXED32:
      case SFIXED32:
        return "integer";
      case INT64:
      case UINT64:
      case SINT64:
      case FIXED64:
      case SFIXED64:
      case FLOAT:
      case DOUBLE:
        return "numeric";
      case BOOL:
        return "boolean";
      case STRING:
      case BYTES:
        return "string";
      default:
        throw Status.fromCode(Status.Code.UNIMPLEMENTED)
            .withDescription(fieldDescriptor.toString() + " is not implemented yet.")
            .asRuntimeException();
    }
  }

  private ObjectNode getJsonRestrictives(
      ObjectMapper mapper, Descriptors.FieldDescriptor.Type fieldDescriptor) {
    final ObjectNode nodeRestrict = mapper.createObjectNode();

    switch (fieldDescriptor) {
      case SFIXED32:
      case INT32:
        nodeRestrict.put("minimum", Integer.MIN_VALUE);
        nodeRestrict.put("maximum", Integer.MAX_VALUE);
        break;
      case SFIXED64:
      case INT64:
        nodeRestrict.put("minimum", Long.MIN_VALUE);
        nodeRestrict.put("maximum", Long.MAX_VALUE);
        break;
      case FIXED32:
      case UINT32:
        nodeRestrict.put("minimum", 0);
        nodeRestrict.put("maximum", UnsignedInteger.MAX_VALUE.longValue());
        break;
      case FIXED64:
      case UINT64:
        nodeRestrict.put("minimum", 0);
        nodeRestrict.put("maximum", Long.parseUnsignedLong("123456789012345678"));
        break;
      case FLOAT:
        nodeRestrict.put("minimum", Float.MIN_VALUE);
        nodeRestrict.put("maximum", Float.MIN_VALUE);
        break;
      case DOUBLE:
        nodeRestrict.put("minimum", Double.MIN_VALUE);
        nodeRestrict.put("maximum", Double.MIN_VALUE);
        break;
      case BYTES:
        nodeRestrict.put("contentEncoding", "base64");
        break;
      default:
        return null;
    }

    return nodeRestrict;
  }

  private ObjectNode toRecord(Descriptors.Descriptor descriptor) {
    ObjectMapper mapper = new ObjectMapper();

    final ObjectNode node = mapper.createObjectNode();
    node.put("title", descriptor.getFullName());
    node.put("type", "object");

    final ObjectNode nodeFields = mapper.createObjectNode();
    descriptor
        .getFields()
        .forEach(
            f -> {
              final ObjectNode nodeFieldType = mapper.createObjectNode();

              String fieldType = toJsonType(f.getType());
              nodeFieldType.put("type", fieldType);

              final ObjectNode nodeRestrict = getJsonRestrictives(mapper, f.getType());
              if (nodeRestrict != null) nodeFieldType.putAll(nodeRestrict);

              nodeFields.put(f.getName(), nodeFieldType);
            });

    node.put("properties", nodeFields);
    return node;
  }
}
