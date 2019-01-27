package io.anemos.metastore.core.proto;

import com.google.gson.Gson;
import com.google.protobuf.Descriptors;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.List;

/**
 * https://avro.apache.org/docs/1.8.2/api/java/org/apache/avro/protobuf/package-summary.html
 */
public class ProtoToAvroSchema {

    Descriptors.Descriptor descriptor;

    public ProtoToAvroSchema(Descriptors.Descriptor descriptor) {
        this.descriptor = descriptor;
    }

    public static String convert(ProtoDescriptor pd, String messageName) {
        Descriptors.Descriptor descriptor = pd.getDescriptorByName(messageName);
        Gson gson = new Gson();
        return gson.toJson(new ProtoToAvroSchema(descriptor).toRecord(descriptor));
    }

    private String toPrimitiveType(Descriptors.FieldDescriptor.Type fieldDescriptor) {
        switch (fieldDescriptor) {
            case UINT64:
            case INT64:
            case FIXED64:
            case SINT64:
            case SFIXED64:
                return "int";
            case INT32:
            case FIXED32:
            case UINT32:
            case SINT32:
            case SFIXED32:
                return "long";
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
                // TODO
                break;
        }
        return null;
    }

    private AvroSchema toRecord(Descriptors.Descriptor descriptor) {
        AvroSchema avroType = new AvroSchema();
        avroType.name = descriptor.getFullName();
        avroType.type = "record";

        descriptor.getFields().forEach(f -> {
            AvroSchema field = new AvroSchema();
            field.name = f.getName();
            switch (f.getType()) {
                case MESSAGE: {
                    field.type = "null";
                    break;
                }
                case ENUM: {
                    throw new NotImplementedException();
                }
                default:
                    if (f.isRepeated()) {
                        field.type = "array";
                        field.items = toPrimitiveType(f.getType());
                    } else {
                        field.type = toPrimitiveType(f.getType());
                    }
                    // TODO: What about MAP?
                    break;
            }


            avroType.addField(field);
        });

        return avroType;
    }

    static class AvroSchema {
        String name;
        Object type = "record";
        String items;
        List<AvroSchema> fields;

        private void addField(AvroSchema field) {
            if (fields == null) {
                fields = new ArrayList<>();
            }
            fields.add(field);
        }
    }
}
