package io.anemos.metastore.core.proto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import io.anemos.metastore.putils.ProtoDomain;
import io.grpc.Status;
import java.io.IOException;
import java.util.*;

/** https://avro.apache.org/docs/1.8.2/api/java/org/apache/avro/protobuf/package-summary.html */
public class ProtoToAvroSchema {

  Descriptors.Descriptor descriptor;

  public ProtoToAvroSchema(Descriptors.Descriptor descriptor) {
    this.descriptor = descriptor;
  }

  public static String convert(ProtoDomain pd, String messageName) throws IOException {
    Descriptors.Descriptor descriptor = pd.getDescriptorByName(messageName);
    ObjectMapper mapper =
        new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    final AvroSchema avroSchema = new ProtoToAvroSchema(descriptor).toRecord(descriptor);
    return mapper.writeValueAsString(avroSchema);
  }

  private AvroSchema toRecord(Descriptors.Descriptor descriptor) {
    AvroSchema avroType = toRecordFields(descriptor, descriptor.getFile().getPackage());

    return avroType;
  }

  private AvroSchema toRecordFields(Descriptors.Descriptor descriptor, String namespace) {
    AvroSchema avroType = new AvroSchema();
    avroType.setType("record");
    if (!namespace.isEmpty()) {
      avroType.setNamespace(namespace);
    }
    avroType.setName(descriptor.getName());
    avroType.setOptions(toOptions(descriptor.getOptions().getAllFields(), true));

    descriptor
        .getFields()
        .forEach(
            f -> {
              AvroSchemaItem field = new AvroSchemaItem();
              field.setName(f.getName());
              field.setOptions(toOptions(f.getOptions().getAllFields(), true));
              switch (f.getType()) {
                case MESSAGE:
                  if (f.isMapField()) {
                    AvroSchemaItem mapField = new AvroSchemaItem();
                    mapField.setType("map");
                    mapField.setValues(
                        toPrimitiveType(f.getMessageType().findFieldByName("value").getType()));
                    field.setType(mapField);
                  } else if (f.isRepeated()) {
                    field.setType(toArrayField(toComplexRecord(f.getMessageType())));
                  } else {
                    field.setType(toComplexRecord(f.getMessageType()));
                  }
                  break;
                case ENUM:
                  if (f.isRepeated()) {
                    field.setType(
                        toArrayField(
                            toEnumField(
                                Arrays.asList(f.getEnumType()), f.getEnumType().getFullName())));
                  } else {
                    field.setType(
                        toEnumField(Arrays.asList(f.getEnumType()), f.getEnumType().getFullName()));
                  }
                  break;
                default:
                  if (f.isRepeated()) {
                    field.setType(toArrayField(toPrimitiveType(f.getType())));
                  } else {
                    field.setType(toPrimitiveType(f.getType()));
                  }
                  break;
              }

              avroType.addField(field);
            });

    return avroType;
  }

  private String toPrimitiveType(Descriptors.FieldDescriptor.Type fieldDescriptor) {
    switch (fieldDescriptor) {
      case UINT64:
      case INT64:
      case FIXED64:
      case SINT64:
      case SFIXED64:
        return "long";
      case INT32:
      case FIXED32:
      case UINT32:
      case SINT32:
      case SFIXED32:
        return "int";
      case BOOL:
        return "boolean";
      case STRING:
        return "string";
      case DOUBLE:
        return "double";
      case FLOAT:
        return "float";
      case BYTES:
        return "bytes";
      default:
        throw Status.fromCode(Status.Code.UNIMPLEMENTED)
            .withDescription(fieldDescriptor.name() + " is not implemented yet.")
            .asRuntimeException();
    }
  }

  private Object toComplexRecord(Descriptors.Descriptor messageType) {

    switch (messageType.getFullName()) {
      case "google.protobuf.Timestamp":
        AvroSchemaItem field = new AvroSchemaItem();
        field.setType("long");
        field.setLogicalType("timestamp-millis");
        return field;
      case "google.protobuf.StringValue":
        return Arrays.asList("null", "string");
      case "google.protobuf.UInt32Value":
      case "google.protobuf.Int32Value":
        return Arrays.asList("null", "int");
      case "google.protobuf.Int64Value":
      case "google.protobuf.UInt64Value":
        return Arrays.asList("null", "long");
      case "google.protobuf.Duration":
        field = new AvroSchemaItem();
        field.setType("fixed");
        field.setSize(12);
        field.setName("Duration");
        return field;
      case "google.protobuf.BoolValue":
        return Arrays.asList("null", "boolean");
      case "google.protobuf.BytesValue":
        return Arrays.asList("null", "bytes");
      case "google.protobuf.DoubleValue":
        return Arrays.asList("null", "double");
      case "google.protobuf.FloatValue":
        return Arrays.asList("null", "float");
      default:
        if (messageType.getEnumTypes().size() > 0) {
          return toEnumField(messageType.getEnumTypes(), messageType.getFullName());
        } else if (messageType.getFields().size() > 0) {
          return toRecordFields(messageType, "");
        } else {
          throw Status.fromCode(Status.Code.UNIMPLEMENTED)
              .withDescription(messageType.getFullName() + " is not implemented yet.")
              .asRuntimeException();
        }
    }
  }

  private AvroSchemaItem toEnumField(
      List<Descriptors.EnumDescriptor> enumTypes, String enumFullName) {
    AvroSchemaItem enumField = new AvroSchemaItem();

    final Optional<Descriptors.EnumDescriptor> first =
        enumTypes.stream().filter(f -> f.getFullName().startsWith(enumFullName)).findFirst();

    enumField.setType("enum");
    enumField.setName(first.get().getName());

    List<String> values = new ArrayList<>();
    first.get().getValues().forEach(item -> values.add(item.getName()));
    enumField.setSymbols(values);

    return enumField;
  }

  private AvroSchemaItem toArrayField(Object items) {
    AvroSchemaItem arrayItem = new AvroSchemaItem();
    arrayItem.setType("array");
    arrayItem.setItems(items);

    return arrayItem;
  }

  private Map<String, Object> toOptions(
      Map<Descriptors.FieldDescriptor, Object> optionFields, Boolean useFullName) {
    if (optionFields.size() == 0) {
      return null;
    }

    Map<String, Object> options = new HashMap<>();
    optionFields.forEach(
        (k, v) -> {
          if (v.getClass().getName().equals("com.google.protobuf.DynamicMessage")) {
            options.put(k.getFullName(), toOptions((((DynamicMessage) v).getAllFields()), false));
          } else {
            final String fieldName = useFullName ? k.getFullName() : k.getName();
            options.put(fieldName, v);
          }
        });

    return options;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  static class AvroSchema {
    private Object type;
    private String namespace;
    private String name;
    private Map<String, Object> options;
    private List<AvroSchemaItem> fields;

    public void addField(AvroSchemaItem field) {
      if (fields == null) {
        fields = new ArrayList<>();
      }
      fields.add(field);
    }

    public List<AvroSchemaItem> getFields() {
      return this.fields;
    }

    public Object getType() {
      return type;
    }

    public void setType(Object type) {
      this.type = type;
    }

    public String getNamespace() {
      return namespace;
    }

    public void setNamespace(String namespace) {
      this.namespace = namespace;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    @JsonAnyGetter
    public Map<String, Object> getOptions() {
      return this.options;
    }

    public void setOptions(Map<String, Object> options) {
      this.options = options;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  static class AvroSchemaItem {
    private String name;
    private Object type;
    private Object items;
    private String logicalType;
    private Integer size;
    private List<String> symbols;
    private String values;
    private Map<String, Object> options;

    public String getName() {
      return this.name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Object getType() {
      return type;
    }

    public void setType(Object type) {
      this.type = type;
    }

    public Object getItems() {
      return items;
    }

    public void setItems(Object items) {
      this.items = items;
    }

    public String getLogicalType() {
      return logicalType;
    }

    public void setLogicalType(String logicalType) {
      this.logicalType = logicalType;
    }

    public Integer getSize() {
      return size;
    }

    public void setSize(Integer size) {
      this.size = size;
    }

    public List<String> getSymbols() {
      return symbols;
    }

    public void setSymbols(List<String> symbols) {
      this.symbols = symbols;
    }

    public String getValues() {
      return values;
    }

    public void setValues(String values) {
      this.values = values;
    }

    @JsonAnyGetter
    public Map<String, Object> getOptions() {
      return this.options;
    }

    public void setOptions(Map<String, Object> options) {
      this.options = options;
    }
  }
}
