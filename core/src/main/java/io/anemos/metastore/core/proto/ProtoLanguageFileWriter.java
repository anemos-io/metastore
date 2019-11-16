package io.anemos.metastore.core.proto;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProtoLanguageFileWriter {
  private Descriptors.FileDescriptor fd;
  private PContainer domain;

  private ProtoLanguageFileWriter(Descriptors.FileDescriptor fileDescriptor, PContainer domain) {
    this.fd = fileDescriptor;
    this.domain = domain;
    if (domain == null) {
      this.domain = new PContainer();
    }
  }

  private ProtoLanguageFileWriter(Descriptors.FileDescriptor fileDescriptor) {
    this(fileDescriptor, null);
  }

  public static void write(
      Descriptors.FileDescriptor fd, PContainer PContainer, OutputStream outputStream) {
    PrintWriter printWriter = new PrintWriter(outputStream);
    new ProtoLanguageFileWriter(fd, PContainer).write(printWriter);
    printWriter.flush();
  }

  public static String write(Descriptors.FileDescriptor fd, PContainer PContainer) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    write(fd, PContainer, byteArrayOutputStream);
    return byteArrayOutputStream.toString();
  }

  public static void write(Descriptors.FileDescriptor fd, OutputStream outputStream) {
    PrintWriter printWriter = new PrintWriter(outputStream);
    new ProtoLanguageFileWriter(fd).write(printWriter);
    printWriter.flush();
  }

  public static String write(Descriptors.FileDescriptor fd) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    write(fd, byteArrayOutputStream);
    return byteArrayOutputStream.toString();
  }

  private static String unsignedToString(final long value) {
    if (value >= 0) {
      return Long.toString(value);
    } else {
      // Pull off the most-significant bit so that BigInteger doesn't think
      // the number is negative, then set it again using setBit().
      return BigInteger.valueOf(value & 0x7FFFFFFFFFFFFFFFL).setBit(63).toString();
    }
  }

  public void write(PrintWriter writer) {
    new ProtoFilePrintWriter(writer).write();
  }

  private class ProtoFilePrintWriter {

    private PrintWriter writer;

    public ProtoFilePrintWriter(PrintWriter writer) {
      this.writer = writer;
    }

    private void extensions() {
      Map<String, List<Descriptors.FieldDescriptor>> extensions = new HashMap<>();
      for (Descriptors.FieldDescriptor fieldDescriptor : fd.getExtensions()) {
        String toExtend = fieldDescriptor.getContainingType().getFullName();
        if (extensions.containsKey(toExtend)) {
          extensions.get(toExtend).add(fieldDescriptor);
        } else {
          ArrayList<Descriptors.FieldDescriptor> fdList = new ArrayList<>();
          fdList.add(fieldDescriptor);
          extensions.put(toExtend, fdList);
        }
      }

      for (String toExtend : extensions.keySet()) {
        writer.println();
        writer.println("extend " + toExtend + " {");
        for (Descriptors.FieldDescriptor fieldDescriptor : extensions.get(toExtend)) {
          field(fieldDescriptor, 1);
        }
        writer.println("}\n");
      }
    }

    private void indent(int indent) {
      for (int i = 0; i < indent; i++) {
        writer.print("\t");
      }
    }

    private boolean isMap(Descriptors.FieldDescriptor field) {
      if (field.getType() != Descriptors.FieldDescriptor.Type.MESSAGE) {
        return false;
      }
      return field.getMessageType().getOptions().getMapEntry();
    }

    private void writeFieldType(Descriptors.FieldDescriptor field) {
      if (field.isRepeated() && !isMap(field)) {
        writer.print("repeated ");
      }
      switch (field.getType()) {
        case UINT64:
          writer.print("uint64");
          break;
        case INT32:
          writer.print("int32");
          break;
        case INT64:
          writer.print("int64");
          break;
        case FIXED64:
          writer.print("fixed64");
          break;
        case FIXED32:
          writer.print("fixed32");
          break;
        case BOOL:
          writer.print("bool");
          break;
        case STRING:
          writer.print("string");
          break;
        case GROUP:
          // TODO figure out if we need to support this (proto2)
          writer.print("GROUP");
          break;
        case MESSAGE:
          {
            Descriptors.Descriptor messageType = field.getMessageType();
            if (messageType.getFile() == fd) {
              if (isMap(field)) {
                writer.print("map<");
                writeFieldType(messageType.findFieldByNumber(1));
                writer.print(", ");
                writeFieldType(messageType.findFieldByNumber(2));
                writer.print(">");
              } else {
                writer.print(messageType.getName());
              }
            } else {
              writer.print(messageType.getFullName());
            }
            break;
          }
        case BYTES:
          writer.print("bytes");
          break;
        case UINT32:
          writer.print("uint32");
          break;
        case ENUM:
          {
            Descriptors.EnumDescriptor enumType = field.getEnumType();
            if (field.getFile() == fd) {
              writer.print(enumType.getName());
            } else {
              writer.print(enumType.getFullName());
            }
            break;
          }
        case SFIXED32:
          writer.print("sfixed32");
          break;
        case SFIXED64:
          writer.print("sfixed64");
          break;
        case SINT32:
          writer.print("sint32");
          break;
        case SINT64:
          writer.print("sint64");
          break;
        case DOUBLE:
          writer.print("double");
          break;
        case FLOAT:
          writer.print("float");
          break;
      }
    }

    private void field(Descriptors.FieldDescriptor field, int indent) {
      indent(indent);
      writeFieldType(field);
      writer.print(" ");
      writer.print(field.getName());
      writer.print(" = ");
      writer.print(field.getNumber());

      Map<Descriptors.FieldDescriptor, Object> resolved = new LinkedHashMap<>();
      resolved.putAll(field.getOptions().getAllFields());
      resolved.putAll(
          convertUnknownFieldValue(
              field.getOptions().getUnknownFields(), domain.getFieldOptionMap()));
      if (resolved.size() > 0) {
        writer.print(" [\n");
        Iterator<Map.Entry<Descriptors.FieldDescriptor, Object>> iterator =
            resolved.entrySet().iterator();
        while (iterator.hasNext()) {
          Map.Entry<Descriptors.FieldDescriptor, Object> fieldOption = iterator.next();
          Descriptors.FieldDescriptor fieldDescriptor = fieldOption.getKey();
          Object value = fieldOption.getValue();
          if (fieldDescriptor.isRepeated()) {
            List values = (List) value;
            for (int i = 0; i < values.size(); i++) {
              Object o = values.get(i);
              indent(indent + 1);
              writeOptionForList(fieldDescriptor, o, indent);
              if (i < values.size() - 1) {
                writer.print(",");
              }
              writer.println();
            }
          } else {
            indent(indent + 1);
            writeOptionForList(fieldDescriptor, value, indent);
            if (iterator.hasNext()) {
              writer.print(",");
            }
            writer.println();
          }
        }
        indent(indent);
        writer.print("]");
      }
      writer.println(";");
    }

    private String value(Descriptors.FieldDescriptor fd, Object value) {
      StringBuilder stringBuilder = new StringBuilder();
      if (fd.getType() == Descriptors.FieldDescriptor.Type.STRING) {
        stringBuilder.append("\"");
        stringBuilder.append(value);
        stringBuilder.append("\"");
      } else {
        stringBuilder.append(value);
      }
      return stringBuilder.toString();
    }

    private void writeValue(Descriptors.FieldDescriptor fd, Object value) {
      if (fd.isRepeated() && value instanceof List) {
        List values = (List) value;
        List<String> stringList = new ArrayList<>();
        writer.print('[');
        values.forEach(v -> stringList.add(value(fd, v)));
        writer.print(String.join(",", stringList));
        writer.print(']');
      } else {
        writer.print(value(fd, value));
      }
    }

    private void writeMessageValue(Message v, int indent) {
      writer.println("{");
      v.getAllFields()
          .forEach(
              (fieldDescriptor, value) -> {
                indent(indent + 1);
                writer.print(fieldDescriptor.getName());
                writer.print(": ");
                if (fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
                  writeMessageValue((Message) value, indent + 1);
                } else {
                  writeValue(fieldDescriptor, value);
                }
                writer.println();
              });
      indent(indent);
      writer.print("}");
    }

    private void write() {
      switch (fd.getSyntax()) {
        case PROTO2:
          writer.println("syntax = \"proto2\";");
          break;
        case PROTO3:
          writer.println("syntax = \"proto3\";");
          break;
        default:
          break;
      }
      writer.println();

      fd.getDependencies()
          .forEach(
              fd -> {
                writer.print("import \"");
                writer.print(fd.getName());
                writer.println("\";");
              });
      writer.println();

      writeOptionsForBlock(fd.getOptions(), 0, "File");

      if (!fd.getPackage().isEmpty()) {
        writer.print("package ");
        writer.print(fd.getPackage());
        writer.println(";");
      }

      extensions();

      for (Descriptors.EnumDescriptor enumDescriptor : fd.getEnumTypes()) {
        writer.println();
        writeEnumDescriptor(enumDescriptor, 0);
      }

      for (Descriptors.ServiceDescriptor serviceDescriptor : fd.getServices()) {
        writer.println();
        writeServiceDescriptor(serviceDescriptor);
      }

      for (Descriptors.Descriptor messageType : fd.getMessageTypes()) {
        writer.println();
        writeMessageDescriptor(messageType, 0);
      }

      List<DescriptorProtos.UninterpretedOption> uninterpretedOptionList =
          fd.getOptions().getUninterpretedOptionList();
      System.out.println();
    }

    private void writeServiceDescriptor(Descriptors.ServiceDescriptor serviceDescriptor) {
      writer.print("service ");
      writer.print(serviceDescriptor.getName());
      writer.println(" {");
      writeOptionsForBlock(serviceDescriptor.getOptions(), 1, "Service");
      for (Descriptors.MethodDescriptor method : serviceDescriptor.getMethods()) {
        indent(1);
        writer.print("rpc ");
        writer.print(method.getName());
        writer.print("(");
        if (method.isClientStreaming()) {
          writer.print("stream ");
        }
        writer.print(method.getInputType().getFullName());
        writer.print(") returns (");
        if (method.isServerStreaming()) {
          writer.print("stream ");
        }
        writer.print(method.getOutputType().getFullName());
        DescriptorProtos.MethodOptions options = method.getOptions();
        if (options.getAllFields().size() == 0) {
          writer.println(") {}");
        } else {
          writer.println(") {");
          writeOptionsForBlock(options, 2, "Method");
          indent(1);
          writer.println("}");
        }
      }

      writer.println("}");
    }

    private void writeEnumDescriptor(Descriptors.EnumDescriptor enumType, int indent) {
      indent(indent);
      writer.print("enum ");
      writer.print(enumType.getName());
      writer.println(" {");
      for (Descriptors.EnumValueDescriptor value : enumType.getValues()) {
        indent(indent + 1);
        writer.print(value.getName());
        writer.print(" = ");
        writer.print(value.getNumber());
        writer.println(";");
      }
      indent(indent);
      writer.println("}");
    }

    private void writeMessageDescriptor(Descriptors.Descriptor messageType, int indent) {
      indent(indent);
      writer.print("message ");
      writer.print(messageType.getName());
      writer.println(" {");

      writeOptionsForBlock(messageType.getOptions(), indent + 1, "Message");

      for (Descriptors.Descriptor nestedType : messageType.getNestedTypes()) {
        if (!nestedType.getOptions().getMapEntry()) {
          writeMessageDescriptor(nestedType, indent + 1);
          writer.println();
        }
      }
      for (Descriptors.EnumDescriptor enumType : messageType.getEnumTypes()) {
        writeEnumDescriptor(enumType, indent + 1);
        writer.println();
      }

      Map<Integer, Descriptors.OneofDescriptor> oneofmap = new HashMap<>();
      for (Descriptors.OneofDescriptor oneof : messageType.getOneofs()) {
        oneofmap.put(oneof.getField(0).getIndex(), oneof);
      }

      List<Descriptors.FieldDescriptor> fields = messageType.getFields();
      for (int ix = 0; ix < fields.size(); ) {
        Descriptors.FieldDescriptor field = fields.get(ix);
        Descriptors.OneofDescriptor oneof = oneofmap.get(ix);
        if (oneof != null) {
          indent(indent + 1);
          writer.print("oneof ");
          writer.print(oneof.getName());
          writer.println(" {");
          for (Descriptors.FieldDescriptor oneofField : oneof.getFields()) {
            field(oneofField, indent + 2);
            ix++;
          }
          indent(indent + 1);
          writer.println("}");
        } else {
          field(field, indent + 1);
          ix++;
        }
      }

      indent(indent);
      writer.println("}");
    }

    private void writeOptionForMethod(
        Descriptors.FieldDescriptor fieldDescriptor, Object value, int indent, String optionType) {
      indent(indent);
      writer.print("option ");
      if (fieldDescriptor.getFullName().startsWith("google.protobuf." + optionType + "Options")) {
        writer.print(fieldDescriptor.getName());
      } else {
        writer.print("(");
        writer.print(fieldDescriptor.getFullName());
        writer.print(")");
      }
      writer.print(" = ");
      if (fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
        writeMessageValue((Message) value, indent);
      } else {
        writeValue(fieldDescriptor, value);
      }
      writer.println(";");
    }

    private void writeOptionForList(
        Descriptors.FieldDescriptor fieldDescriptor, Object value, int indent) {
      if (fieldDescriptor.getFullName().startsWith("google.protobuf.FieldOptions")) {
        writer.print(fieldDescriptor.getName());
      } else {
        writer.print("(");
        writer.print(fieldDescriptor.getFullName());
        writer.print(")");
      }
      writer.print(" = ");
      if (fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
        writeMessageValue((Message) value, indent + 1);
      } else {
        writeValue(fieldDescriptor, value);
      }
    }

    private void writeOptionsForBlock(
        com.google.protobuf.GeneratedMessageV3.ExtendableMessage options,
        int indent,
        String optionMap) {

      Map<Integer, Descriptors.FieldDescriptor> unknownMap;
      switch (optionMap) {
        case "File":
          unknownMap = domain.getFileOptionMap();
          break;
        case "Message":
          unknownMap = domain.getMessageOptionMap();
          break;
        case "Field":
          unknownMap = domain.getFieldOptionMap();
          break;
        case "Enum":
          unknownMap = domain.getEnumOptionMap();
          break;
        case "EnumValue":
          unknownMap = domain.getEnumValueOptionMap();
          break;
        case "Service":
          unknownMap = domain.getServiceOptionMap();
          break;
        case "Method":
          unknownMap = domain.getMethodOptionMap();
          break;
        default:
          throw new RuntimeException("Exception");
      }

      Map<Descriptors.FieldDescriptor, Object> resolved = new LinkedHashMap<>();
      resolved.putAll(options.getAllFields());
      resolved.putAll(convertUnknownFieldValue(options.getUnknownFields(), unknownMap));

      resolved.forEach(
          (fieldDescriptor, value) -> {
            if (fieldDescriptor.isRepeated()) {
              List values = (List) value;
              values.forEach(v -> writeOptionForMethod(fieldDescriptor, v, indent, optionMap));
            } else {
              writeOptionForMethod(fieldDescriptor, value, indent, optionMap);
            }
          });

      writer.println();
    }

    private Object convertFieldValue(Descriptors.FieldDescriptor fieldDescriptor, Object value) {
      switch (fieldDescriptor.getType()) {
        case MESSAGE:
          try {
            DynamicMessage dynamicMessage =
                DynamicMessage.parseFrom(fieldDescriptor.getMessageType(), (ByteString) value);
            return dynamicMessage;
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        case BOOL:
          return value.equals(1L);
        case ENUM:
        case STRING:
          ByteString byteString = (ByteString) value;
          return byteString.toStringUtf8();
        case INT32:
        case INT64:
          return unsignedToString((Long) value);
        case DOUBLE:
          return Double.longBitsToDouble((Long) value);
        case FLOAT:
          return Float.intBitsToFloat((Integer) value);
      }
      throw new RuntimeException(
          "conversion of unknownfield for type "
              + fieldDescriptor.getType().toString()
              + " not implemented");
    }

    private Map<Descriptors.FieldDescriptor, Object> convertUnknownFieldValue(
        UnknownFieldSet unknownFieldSet, Map<Integer, Descriptors.FieldDescriptor> optionsMap) {
      Map<Descriptors.FieldDescriptor, Object> unknownFieldValues = new LinkedHashMap<>();
      unknownFieldSet
          .asMap()
          .forEach(
              (number, field) -> {
                Descriptors.FieldDescriptor fieldDescriptor = optionsMap.get(number);
                if (fieldDescriptor.isRepeated()) {
                  unknownFieldValues.put(
                      fieldDescriptor, convertUnknownFieldList(fieldDescriptor, field));
                } else {
                  unknownFieldValues.put(
                      fieldDescriptor, convertUnknownFieldValue(fieldDescriptor, field));
                }
              });
      return unknownFieldValues;
    }

    /**
     * https://developers.google.com/protocol-buffers/docs/encoding#structure
     *
     * @param fieldDescriptor
     * @param field
     * @return
     */
    private Object convertUnknownFieldList(
        Descriptors.FieldDescriptor fieldDescriptor, UnknownFieldSet.Field field) {
      List list = new ArrayList();
      if (field.getLengthDelimitedList().size() > 0) {
        field
            .getLengthDelimitedList()
            .forEach(value -> list.add(convertFieldValue(fieldDescriptor, value)));
      }
      if (field.getFixed32List().size() > 0) {
        field
            .getFixed32List()
            .forEach(value -> list.add(convertFieldValue(fieldDescriptor, value)));
      }
      if (field.getFixed64List().size() > 0) {
        field
            .getFixed64List()
            .forEach(value -> list.add(convertFieldValue(fieldDescriptor, value)));
      }
      if (field.getVarintList().size() > 0) {
        field.getVarintList().forEach(value -> list.add(convertFieldValue(fieldDescriptor, value)));
      }
      if (field.getGroupList().size() > 0) {
        throw new RuntimeException("Groups are not implemented");
      }
      return list;
    }

    private Object convertUnknownFieldValue(
        Descriptors.FieldDescriptor fieldDescriptor, UnknownFieldSet.Field field) {

      if (field.getLengthDelimitedList().size() > 0) {
        if (field.getLengthDelimitedList().size() > 1) {
          throw new RuntimeException(
              "Single value should not contrain more then 1 value in the unknown field");
        }
        return convertFieldValue(fieldDescriptor, field.getLengthDelimitedList().get(0));
      }
      if (field.getFixed32List().size() > 0) {
        if (field.getFixed32List().size() > 1) {
          throw new RuntimeException(
              "Single value should not contrain more then 1 value in the unknown field");
        }
        return convertFieldValue(fieldDescriptor, field.getFixed32List().get(0));
      }
      if (field.getFixed64List().size() > 0) {
        if (field.getFixed64List().size() > 1) {
          throw new RuntimeException(
              "Single value should not contrain more then 1 value in the unknown field");
        }
        return convertFieldValue(fieldDescriptor, field.getFixed64List().get(0));
      }
      if (field.getVarintList().size() > 0) {
        if (field.getVarintList().size() > 1) {
          throw new RuntimeException(
              "Single value should not contrain more then 1 value in the unknown field");
        }
        return convertFieldValue(fieldDescriptor, field.getVarintList().get(0));
      }
      if (field.getGroupList().size() > 0) {
        throw new RuntimeException("Groups are not implemented");
      }
      return null;
    }
  }
}
