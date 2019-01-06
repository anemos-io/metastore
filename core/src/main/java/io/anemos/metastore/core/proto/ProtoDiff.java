package io.anemos.metastore.core.proto;

import com.google.protobuf.DescriptorProtos;

import java.util.*;

public class ProtoDiff {

    private DescriptorProtos.FileDescriptorProto fd_ref;
    private DescriptorProtos.FileDescriptorProto fd_new;

    ProtoDiff(DescriptorProtos.FileDescriptorProto fd_ref, DescriptorProtos.FileDescriptorProto fd_new) {
        this.fd_ref = fd_ref;
        this.fd_new = fd_new;
    }

    public void diff() {

        diffMessageTypes(fd_ref.getMessageTypeList(), fd_new.getMessageTypeList());
        diffEnumTypes(fd_ref.getEnumTypeList(), fd_new.getEnumTypeList());
        diffServices(fd_ref.getServiceList(), fd_new.getServiceList());


    }

    private void diffServices(List<DescriptorProtos.ServiceDescriptorProto> s_ref, List<DescriptorProtos.ServiceDescriptorProto> s_new) {

    }

    private void diffEnumTypes(List<DescriptorProtos.EnumDescriptorProto> e_ref, List<DescriptorProtos.EnumDescriptorProto> e_new) {

    }

    private Map<String, DescriptorProtos.DescriptorProto> toMap4Descriptor(List<DescriptorProtos.DescriptorProto> in) {
        Map<String, DescriptorProtos.DescriptorProto> out = new HashMap<>();
        in.forEach(descriptor -> {
            out.put(descriptor.getName(), descriptor);
        });
        return out;
    }

    private Set<String> onlyInLeft(Map<String, ?> left, Map<String, ?> right) {
        HashSet<String> strings = new HashSet<>(left.keySet());
        strings.removeAll(right.keySet());
        return strings;
    }

    private Set<String> onlyInCommon(Map<String, ?> left, Map<String, ?> right) {
        Set<String> intersect = new HashSet<>(left.keySet());
        intersect.addAll(right.keySet());

        intersect.removeAll(onlyInLeft(left, right));
        intersect.removeAll(onlyInLeft(right, left));
        return intersect;
    }


    private void diffMessageTypes(List<DescriptorProtos.DescriptorProto> mt_ref, List<DescriptorProtos.DescriptorProto> mt_new) {
        Map<String, DescriptorProtos.DescriptorProto> m_ref = toMap4Descriptor(mt_ref);
        Map<String, DescriptorProtos.DescriptorProto> m_new = toMap4Descriptor(mt_new);

        Set<String> onlyRef = onlyInLeft(m_ref, m_new);
        Set<String> onlyNew = onlyInLeft(m_new, m_ref);
        Set<String> common = onlyInCommon(m_new, m_ref);

        common.forEach(k -> {
            diffMessageType(m_ref.get(k), m_new.get(k));
        });
    }

    private Map<String, DescriptorProtos.FieldDescriptorProto> toMap4FieldDescriptor(List<DescriptorProtos.FieldDescriptorProto> in) {
        Map<String, DescriptorProtos.FieldDescriptorProto> out = new HashMap<>();
        in.forEach(descriptor -> {
            out.put(descriptor.getName(), descriptor);
        });
        return out;
    }


    private void diffMessageType(DescriptorProtos.DescriptorProto d_ref, DescriptorProtos.DescriptorProto d_new) {
        diffFields(d_ref.getFieldList(), d_new.getFieldList());
    }

    private void diffFields(List<DescriptorProtos.FieldDescriptorProto> f_ref, List<DescriptorProtos.FieldDescriptorProto> f_new) {
        Map<String, DescriptorProtos.FieldDescriptorProto> m_ref = toMap4FieldDescriptor(f_ref);
        Map<String, DescriptorProtos.FieldDescriptorProto> m_new = toMap4FieldDescriptor(f_new);

        Set<String> onlyRef = onlyInLeft(m_ref, m_new);
        Set<String> onlyNew = onlyInLeft(m_new, m_ref);
        Set<String> common = onlyInCommon(m_new, m_ref);

        common.forEach(k -> {
            diffField(m_ref.get(k), m_new.get(k));
        });
    }

    private void diffField(DescriptorProtos.FieldDescriptorProto f_ref, DescriptorProtos.FieldDescriptorProto f_new) {

        if(f_ref.equals(f_new)) {
            System.out.println("equal");
        }
        else {
            System.out.println("NOT");
        }

    }


//
//
//
//
//
//
//
//
//
//
//
//    private Descriptors.FileDescriptor fd;
//
//
//    public ProtoDiff(Descriptors.FileDescriptor fd) {
//        this.fd = fd;
//    }
//
//
//    public static void write(Descriptors.FileDescriptor fd, OutputStream outputStream) {
//        PrintWriter printWriter = new PrintWriter(outputStream);
//        new ProtoDiff(fd).write(printWriter);
//        printWriter.flush();
//    }
//
//    public static String write(Descriptors.FileDescriptor fd) {
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);
//        write(fd, byteArrayOutputStream);
//        printWriter.flush();
//        return byteArrayOutputStream.toString();
//    }
//
//    public void write(PrintWriter writer) {
//        new ProtoFilePrintWriter(writer).write();
//    }
//
//    private class ProtoFilePrintWriter {
//
//        private PrintWriter writer;
//
//        public ProtoFilePrintWriter(PrintWriter writer) {
//            this.writer = writer;
//        }
//
//        private void fileOptions() {
//            DescriptorProtos.FileOptions options = fdp.getOptions();
//
//            for (Map.Entry<Descriptors.FieldDescriptor, Object> field
//                    : options.getAllFields().entrySet()) {
//                writer.print("option ");
//                writer.print(field.getKey().getName());
//                writer.print(" = ");
//                if (field.getKey().getType() == Descriptors.FieldDescriptor.Type.STRING) {
//                    writer.print("\"");
//                    writer.print(field.getValue());
//                    writer.print("\"");
//                } else {
//                    writer.print(field.getValue());
//                }
//                writer.println(";");
//            }
//            writer.println();
//        }
//
//        private void indent(int indent) {
//            for (int i = 0; i < indent; i++) {
//                writer.print("\t");
//            }
//        }
//
//        private boolean isMap(Descriptors.FieldDescriptor field) {
//            if (field.getType() != Descriptors.FieldDescriptor.Type.MESSAGE) {
//                return false;
//            }
//            return field.getMessageType().getOptions().getMapEntry();
//        }
//
//        private void writeFieldType(Descriptors.FieldDescriptor field) {
//            if (field.isRepeated() && !isMap(field)) {
//                writer.print("repeated ");
//            }
//            switch (field.getType()) {
//                case UINT64:
//                    writer.print("uint64");
//                    break;
//                case INT32:
//                    writer.print("int32");
//                    break;
//                case INT64:
//                    writer.print("int64");
//                    break;
//                case FIXED64:
//                    writer.print("fixed64");
//                    break;
//                case FIXED32:
//                    writer.print("fixed32");
//                    break;
//                case BOOL:
//                    writer.print("bool");
//                    break;
//                case STRING:
//                    writer.print("string");
//                    break;
//                case GROUP:
//                    // TODO
//                    writer.print("GROUP");
//                    break;
//                case MESSAGE: {
//                    Descriptors.Descriptor messageType = field.getMessageType();
//                    if (field.getFile() == fd) {
//                        if (isMap(field)) {
//                            writer.print("map<");
//                            writeFieldType(messageType.findFieldByNumber(1));
//                            writer.print(", ");
//                            writeFieldType(messageType.findFieldByNumber(2));
//                            writer.print(">");
//                        } else {
//                            writer.print(messageType.getName());
//                        }
//                    } else {
//                        writer.print(messageType.getFullName());
//                    }
//                    break;
//                }
//                case BYTES:
//                    writer.print("bytes");
//                    break;
//                case UINT32:
//                    writer.print("uint32");
//                    break;
//                case ENUM: {
//                    Descriptors.EnumDescriptor enumType = field.getEnumType();
//                    if (field.getFile() == fd) {
//                        writer.print(enumType.getName());
//                    } else {
//                        writer.print(enumType.getFullName());
//                    }
//                    break;
//                }
//                case SFIXED32:
//                    writer.print("sfixed32");
//                    break;
//                case SFIXED64:
//                    writer.print("sfixed64");
//                    break;
//                case SINT32:
//                    writer.print("sint32");
//                    break;
//                case SINT64:
//                    writer.print("sint64");
//                    break;
//                case DOUBLE:
//                    writer.print("double");
//                    break;
//                case FLOAT:
//                    writer.print("float");
//                    break;
//            }
//        }
//
//        private void field(Descriptors.FieldDescriptor field, int indent) {
//            indent(indent);
//            writeFieldType(field);
//            writer.print(" ");
//            writer.print(field.getName());
//            writer.print(" = ");
//            writer.print(field.getNumber());
//
//            field.getOptions().getAllFields().forEach(
//                    (k, v) -> {
//                        writer.print(" [");
//                        if (k.getFullName().startsWith("google.protobuf.FieldOptions")) {
//                            writer.print(k.getName());
//                        } else {
//                            writer.print("(");
//                            writer.print(k.getFullName());
//                            writer.print(")");
//                        }
//
//                        writer.print(" = ");
//                        value(v);
//                        writer.print("]");
//                    }
//            );
//            writer.println(";");
//        }
//
//        private void value(Object v) {
//            if (v instanceof Message) {
//                message((Message) v);
//            } else {
//                writer.print(v);
//            }
//        }
//
//        private void message(Message v) {
//            writer.println("{");
//            for (Map.Entry<Descriptors.FieldDescriptor, Object> field
//                    : v.getAllFields().entrySet()) {
//                indent(2);
//                writer.print(field.getKey().getName());
//                writer.print(": ");
//                if (field.getKey().getType() == Descriptors.FieldDescriptor.Type.STRING) {
//                    writer.print("\"");
//                    writer.print(field.getValue());
//                    writer.print("\"");
//                } else {
//                    writer.print(field.getValue());
//                }
//            }
//            writer.println();
//            indent(1);
//            writer.print("}");
//        }
//
//        private void write() {
//            switch (fd.getSyntax()) {
//                case PROTO2:
//                    writer.println("syntax = \"proto2\";");
//                    break;
//                case PROTO3:
//                    writer.println("syntax = \"proto3\";");
//                    break;
//                default:
//                    break;
//            }
//            writer.println();
//
//            fd.getDependencies().forEach(fd -> {
//                writer.print("import \"");
//                writer.print(fd.getName());
//                writer.println("\";");
//            });
//            writer.println();
//
//
//            fileOptions();
//
//
//            writer.print("package ");
//            writer.print(fd.getPackage());
//            writer.println(";");
//
//            for (Descriptors.Descriptor messageType : fd.getMessageTypes()) {
//                writer.println();
//                messageType(messageType, 0);
//            }
//
//
//            List<DescriptorProtos.UninterpretedOption> uninterpretedOptionList = fd.getOptions().getUninterpretedOptionList();
//            System.out.println();
//        }
//
//        private void enumType(Descriptors.EnumDescriptor enumType, int indent) {
//            indent(indent);
//            writer.print("enum ");
//            writer.print(enumType.getName());
//            writer.println(" {");
//            for (Descriptors.EnumValueDescriptor value : enumType.getValues()) {
//                indent(indent + 1);
//                writer.print(value.getName());
//                writer.print(" = ");
//                writer.print(value.getNumber());
//                writer.println(";");
//            }
//            indent(indent);
//            writer.println("}");
//        }
//
//        private void messageType(Descriptors.Descriptor messageType, int indent) {
//            indent(indent);
//            writer.print("message ");
//            writer.print(messageType.getName());
//            writer.println(" {");
//            for (Descriptors.Descriptor nestedType : messageType.getNestedTypes()) {
//                if (!nestedType.getOptions().getMapEntry()) {
//                    messageType(nestedType, indent + 1);
//                    writer.println();
//                }
//            }
//            for (Descriptors.EnumDescriptor enumType : messageType.getEnumTypes()) {
//                enumType(enumType, indent + 1);
//                writer.println();
//            }
//
//            Map<Integer, Descriptors.OneofDescriptor> oneofmap = new HashMap<>();
//            for (Descriptors.OneofDescriptor oneof : messageType.getOneofs()) {
//                oneofmap.put(oneof.getField(0).getIndex(), oneof);
//            }
//
//            List<Descriptors.FieldDescriptor> fields = messageType.getFields();
//            for (int ix = 0; ix < fields.size(); ) {
//                Descriptors.FieldDescriptor field = fields.get(ix);
//                Descriptors.OneofDescriptor oneof = oneofmap.get(ix);
//                if (oneof != null) {
//                    indent(indent + 1);
//                    writer.print("oneof ");
//                    writer.print(oneof.getName());
//                    writer.println(" {");
//                    for (Descriptors.FieldDescriptor oneofField : oneof.getFields()) {
//                        field(oneofField, indent + 2);
//                        ix++;
//                    }
//                    indent(indent + 1);
//                    writer.println("}");
//                } else {
//                    field(field, indent + 1);
//                    ix++;
//                }
//            }
//
//            indent(indent);
//            writer.println("}");
//        }
//
//    }

}
