package io.anemos.metastore.core.proto;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProtoLanguageFileWriter {
  private Descriptors.FileDescriptor fd;
  private PContainer PContainer;

  ProtoLanguageFileWriter(Descriptors.FileDescriptor fileDescriptor, PContainer PContainer) {
    this(fileDescriptor);
    this.PContainer = PContainer;
  }

  ProtoLanguageFileWriter(Descriptors.FileDescriptor fileDescriptor) {
    this.fd = fileDescriptor;
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

    private void fileOptions() {
      DescriptorProtos.FileOptions options = fd.toProto().getOptions();

      for (Map.Entry<Descriptors.FieldDescriptor, Object> field :
          options.getAllFields().entrySet()) {
        writeFileOption(field.getKey(), field.getValue(), false);
      }
      if (!options.getUnknownFields().asMap().isEmpty()) {
        HashMultimap<Descriptors.FieldDescriptor, String> unknownOptionsMap =
            getUnknownFieldValues(options.getUnknownFields(), PContainer.getFileOptionMap(), 0);
        Set<Descriptors.FieldDescriptor> keys = unknownOptionsMap.keySet();
        for (Descriptors.FieldDescriptor fd : keys) {
          Collection<String> values = unknownOptionsMap.get(fd);
          for (String value : values) {
            writeFileOption(fd, value, true);
          }
        }
      }
      writer.println();
    }

    private void writeFileOption(Descriptors.FieldDescriptor fd, Object value, boolean unknown) {
      writer.print("option ");
      if (unknown) {
        writer.print("(");
      }
      writer.print(fd.getName());
      if (unknown) {
        writer.print(")");
      }
      writer.print(" = ");
      if (fd.getType() == Descriptors.FieldDescriptor.Type.STRING) {
        if (!unknown) {
          writer.print("\"");
        }
        writer.print(value);
        if (!unknown) {
          writer.print("\"");
        }
      } else {
        writer.print(value);
      }
      writer.println(";");
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

      boolean hasFieldOptions =
          field.getOptions().getAllFields().size() > 0
              || field.getOptions().getUnknownFields().asMap().keySet().size() > 0;
      if (hasFieldOptions) writer.print(" [\n");

      Iterator<Map.Entry<Descriptors.FieldDescriptor, Object>> iter =
          field.getOptions().getAllFields().entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry<Descriptors.FieldDescriptor, Object> fieldOption = iter.next();
        Descriptors.FieldDescriptor fieldDescriptor = fieldOption.getKey();
        Object value = fieldOption.getValue();
        indent(indent + 1);
        if (fieldDescriptor.getFullName().startsWith("google.protobuf.FieldOptions")) {
          writer.print(fieldDescriptor.getName());
        } else {
          writer.print("(");
          writer.print(fieldDescriptor.getFullName());
          writer.print(")");
        }

        writer.print(" = ");
        value(value, fieldDescriptor);

        if (iter.hasNext()) writer.print(", ");
        writer.print("\n");
      }
      if (!field.getOptions().getUnknownFields().asMap().isEmpty()) {
        HashMultimap<Descriptors.FieldDescriptor, String> unknownOptionsMap =
            getUnknownFieldValues(
                field.getOptions().getUnknownFields(), PContainer.getFieldOptionMap(), indent + 1);
        Iterator<Map.Entry<Descriptors.FieldDescriptor, String>> unknownIter =
            unknownOptionsMap.entries().iterator();
        while (unknownIter.hasNext()) {
          Map.Entry<Descriptors.FieldDescriptor, String> fieldOption = unknownIter.next();
          Descriptors.FieldDescriptor fd = fieldOption.getKey();
          String value = fieldOption.getValue();
          indent(indent + 1);
          writer.print("(");
          writer.print(fd.getName());
          writer.print(")");

          writer.print(" = ");
          writer.print(value);

          if (unknownIter.hasNext()) writer.print(",");
          writer.print("\n");
        }
      }

      if (hasFieldOptions) {
        indent(indent);
        writer.print("]");
      }

      writer.println(";");
      if (hasFieldOptions) writer.println();
    }

    private void value(Object v, Descriptors.FieldDescriptor fieldDescriptor) {
      if (v instanceof Message) {
        message((Message) v);
      } else {
        writer.print(getOptionValue(fieldDescriptor, v));
      }
    }

    private void message(Message v) {
      writer.println("{");
      for (Map.Entry<Descriptors.FieldDescriptor, Object> field : v.getAllFields().entrySet()) {
        indent(2);
        writer.print(field.getKey().getName());
        writer.print(": ");
        if (field.getKey().getType() == Descriptors.FieldDescriptor.Type.STRING) {
          writer.print("\"");
          writer.print(field.getValue());
          writer.print("\"");
        } else {
          writer.print(field.getValue());
        }
      }
      writer.println();
      indent(1);
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

      fileOptions();

      if (!fd.getPackage().isEmpty()) {
        writer.print("package ");
        writer.print(fd.getPackage());
        writer.println(";");
      }

      extensions();

      for (Descriptors.EnumDescriptor enumDescriptor : fd.getEnumTypes()) {
        writer.println();
        enumType(enumDescriptor, 0);
      }

      for (Descriptors.ServiceDescriptor serviceDescriptor : fd.getServices()) {
        writer.println();
        serviceType(serviceDescriptor, 0);
      }

      for (Descriptors.Descriptor messageType : fd.getMessageTypes()) {
        writer.println();
        messageType(messageType, 0);
      }

      List<DescriptorProtos.UninterpretedOption> uninterpretedOptionList =
          fd.getOptions().getUninterpretedOptionList();
      System.out.println();
    }

    private void serviceType(Descriptors.ServiceDescriptor serviceDescriptor, int indent) {
      indent(indent);
      writer.print("service ");
      writer.print(serviceDescriptor.getName());
      writer.println(" {");
      for (Descriptors.MethodDescriptor method : serviceDescriptor.getMethods()) {
        indent(indent + 1);
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
        writer.println(")");
      }

      indent(indent);
      writer.println("}");
    }

    private void enumType(Descriptors.EnumDescriptor enumType, int indent) {
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

    private void messageType(Descriptors.Descriptor messageType, int indent) {
      indent(indent);
      writer.print("message ");
      writer.print(messageType.getName());
      writer.println(" {");

      messageOptions(messageType, indent);

      for (Descriptors.Descriptor nestedType : messageType.getNestedTypes()) {
        if (!nestedType.getOptions().getMapEntry()) {
          messageType(nestedType, indent + 1);
          writer.println();
        }
      }
      for (Descriptors.EnumDescriptor enumType : messageType.getEnumTypes()) {
        enumType(enumType, indent + 1);
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

    private void messageOptions(Descriptors.Descriptor messageDescriptor, int indent) {
      Map<Descriptors.FieldDescriptor, Object> allFields =
          messageDescriptor.getOptions().getAllFields();
      allFields.forEach(
          (fieldDescriptor, obj) -> {
            for (Descriptors.FieldDescriptor subFieldDescriptor :
                fieldDescriptor.getMessageType().getFields()) {
              Message optionsMessage = (Message) obj;
              Object value = optionsMessage.getField(subFieldDescriptor);

              if (subFieldDescriptor.isRepeated()) {
                List<Object> repeatedMessage = (List<Object>) value;
                for (Object repeatedObject : repeatedMessage) {
                  String optionsValue = getOptionValue(subFieldDescriptor, repeatedObject);
                  printMessageOption(
                      fieldDescriptor.getFullName(),
                      subFieldDescriptor.getName(),
                      optionsValue,
                      indent + 1);
                }
              } else {
                String optionsValue = getOptionValue(subFieldDescriptor, value);
                printMessageOption(
                    fieldDescriptor.getFullName(),
                    subFieldDescriptor.getName(),
                    optionsValue,
                    indent + 1);
              }
            }
          });
      if (!messageDescriptor.getOptions().getUnknownFields().asMap().isEmpty()) {
        HashMultimap<Descriptors.FieldDescriptor, String> unknownOptionsMap =
            getUnknownFieldValues(
                messageDescriptor.getOptions().getUnknownFields(),
                PContainer.getMessageOptionMap(),
                indent + 1);
        Set<Descriptors.FieldDescriptor> keys = unknownOptionsMap.keySet();
        for (Descriptors.FieldDescriptor fd : keys) {
          Collection<String> values = unknownOptionsMap.get(fd);
          for (String value : values) {
            printMessageOption("", fd.getName(), value, indent + 1);
          }
        }
      }
      writer.println();
    }

    private void printMessageOption(
        String packageName, String optionName, String value, int indent) {
      if (!value.isEmpty()) {
        indent(indent);
        if (!packageName.isEmpty()) {
          packageName = packageName + ".";
        }
        writer.println(String.format("option %s(%s) = %s;", packageName, optionName, value));
      }
    }

    private String getOptionValue(Descriptors.FieldDescriptor descriptor, Object value) {
      switch (descriptor.getType()) {
        case STRING:
          return "\"" + (String) value + "\"";
        case INT32:
          return Integer.toString((Integer) value);
        case INT64:
          return Long.toString((Long) value);
        case ENUM:
          Descriptors.EnumValueDescriptor valueDescriptor = (Descriptors.EnumValueDescriptor) value;
          return valueDescriptor.toString();
      }
      return "";
    }

    private String getUnknownPrimitiveFieldValue(
        Descriptors.FieldDescriptor fieldDescriptor, Object value, int indent) {
      switch (fieldDescriptor.getType()) {
        case MESSAGE:
          try {
            StringBuilder stringBuilder = new StringBuilder();
            DynamicMessage dynamicMessage =
                DynamicMessage.parseFrom(fieldDescriptor.getMessageType(), (ByteString) value);
            stringBuilder.append("{\n");

            Iterator<Descriptors.FieldDescriptor> iter =
                dynamicMessage.getAllFields().keySet().iterator();
            while (iter.hasNext()) {
              Descriptors.FieldDescriptor fd = iter.next();
              Object fieldValue = dynamicMessage.getField(fd);
              for (int i = 0; i < indent + 1; i++) {
                stringBuilder.append("\t");
              }
              stringBuilder.append(fd.getName());
              stringBuilder.append(": ");

              if (fd.isRepeated()) {
                stringBuilder.append("[");
                List<Object> repeatedValues = (List<Object>) fieldValue;
                Iterator<Object> repeatedIt = repeatedValues.iterator();
                while (repeatedIt.hasNext()) {
                  stringBuilder.append(getOptionValue(fd, repeatedIt.next()));
                  if (repeatedIt.hasNext()) {
                    stringBuilder.append(",");
                  } else {
                    stringBuilder.append("]");
                  }
                }
              } else {
                String optionValue = getOptionValue(fd, fieldValue);
                stringBuilder.append(optionValue);
              }
              if (iter.hasNext()) {
                stringBuilder.append(",");
              }
              stringBuilder.append("\n");
            }
            for (int i = 0; i < indent; i++) {
              stringBuilder.append("\t");
            }
            stringBuilder.append("}");
            return stringBuilder.toString();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        case BOOL:
          return value.equals(1L) ? "true" : "false";
        case ENUM:
        case STRING:
          ByteString byteString = (ByteString) value;
          return "\"" + byteString.toStringUtf8() + "\"";
        case INT32:
        case INT64:
          return unsignedToString((Long) value);
        case DOUBLE:
          Double d = Double.longBitsToDouble((Long) value);
          return d.toString();
        case FLOAT:
          Float f = Float.intBitsToFloat((Integer) value);
          return f.toString();
      }
      throw new RuntimeException(
          "conversion of unknownfield for type "
              + fieldDescriptor.getType().toString()
              + " not implemented");
    }

    private HashMultimap<Descriptors.FieldDescriptor, String> getUnknownFieldValues(
        UnknownFieldSet unknownFieldSet,
        Map<Integer, Descriptors.FieldDescriptor> optionsMap,
        int indent) {
      HashMultimap<Descriptors.FieldDescriptor, String> unknownFieldValues = HashMultimap.create();
      unknownFieldSet
          .asMap()
          .forEach(
              (number, field) -> {
                Descriptors.FieldDescriptor fieldDescriptor = optionsMap.get(number);
                unknownFieldValues.putAll(getUnknownFieldValue(fieldDescriptor, field, indent));
              });
      return unknownFieldValues;
    }

    private Multimap<Descriptors.FieldDescriptor, String> getUnknownFieldValue(
        Descriptors.FieldDescriptor fieldDescriptor, UnknownFieldSet.Field field, int indent) {
      HashMultimap<Descriptors.FieldDescriptor, String> unknownFieldValues = HashMultimap.create();
      for (Object value : field.getLengthDelimitedList()) {
        unknownFieldValues.put(
            fieldDescriptor, getUnknownPrimitiveFieldValue(fieldDescriptor, value, indent));
      }
      for (Object value : field.getFixed32List()) {
        unknownFieldValues.put(
            fieldDescriptor, getUnknownPrimitiveFieldValue(fieldDescriptor, value, indent));
      }
      for (Object value : field.getFixed64List()) {
        unknownFieldValues.put(
            fieldDescriptor, getUnknownPrimitiveFieldValue(fieldDescriptor, value, indent));
      }
      for (Object value : field.getVarintList()) {
        unknownFieldValues.put(
            fieldDescriptor, getUnknownPrimitiveFieldValue(fieldDescriptor, value, indent));
      }
      for (Object value : field.getGroupList()) {
        unknownFieldValues.put(
            fieldDescriptor, getUnknownPrimitiveFieldValue(fieldDescriptor, value, indent));
      }
      return unknownFieldValues;
    }
  }
}
