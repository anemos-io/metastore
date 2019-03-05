package io.anemos.metastore.core.proto;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.CaseFormat;
import com.google.protobuf.*;

public class AvroToProtoSchema {

    private JsonNode node;

    public AvroToProtoSchema(JsonNode node) {
        this.node = node;
    }

    public DescriptorProtos.FileDescriptorProto toDescriptor(){
        String recordName = node.path("name").asText();
        String recordNamespace = node.path("namespace").asText();
        //String recordType = node.path("type").asText();
        JsonNode fields = node.withArray("fields");

        DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder =
                DescriptorProtos.FileDescriptorProto.newBuilder()
                        .setName(recordName)
                        .setPackage(recordNamespace)
                        .setSyntax("proto3");

        DescriptorProtos.DescriptorProto.Builder descriptor = DescriptorProtos.DescriptorProto.newBuilder()
                .setName(recordName);

        int i = 1;
        for (JsonNode field: fields){
            String fieldName = field.path("name").asText();
            String fieldType = field.path("type").asText();

            DescriptorProtos.FieldDescriptorProto fieldDescriptorProtoBuilder = DescriptorProtos.FieldDescriptorProto.newBuilder()
                    .setType(toFieldDescriptorType(fieldType))
                    .setName(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName))
                    .setNumber(i).build();
            descriptor.addField(fieldDescriptorProtoBuilder);
            i++;
        }

        fileDescriptorProtoBuilder.addMessageType(descriptor.build());

        return fileDescriptorProtoBuilder.build();

    }

    //TODO: Continue with existing mappings on FieldDescriptor
    private DescriptorProtos.FieldDescriptorProto.Type toFieldDescriptorType(String jsonType){
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



}
