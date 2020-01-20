package io.anemos.metastore.putils;

import com.google.common.base.Strings;
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
  private ProtoDomain domain;

  private ProtoLanguageFileWriter(Descriptors.FileDescriptor fileDescriptor, ProtoDomain domain) {
    this.fd = fileDescriptor;
    this.domain = domain;
    if (domain == null) {
      this.domain = new ProtoDomain();
    }
  }

  private ProtoLanguageFileWriter(Descriptors.FileDescriptor fileDescriptor) {
    this(fileDescriptor, null);
  }

  public static void write(
      Descriptors.FileDescriptor fd, ProtoDomain PContainer, OutputStream outputStream) {
    PrintWriter printWriter = new PrintWriter(outputStream);
    new ProtoLanguageFileWriter(fd, PContainer).write(printWriter);
    printWriter.flush();
  }

  public static String write(Descriptors.FileDescriptor fd, ProtoDomain PContainer) {
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
    private CommentIndexer commentIndexer;

    public ProtoFilePrintWriter(PrintWriter writer) {
      this.writer = writer;
      this.commentIndexer = new CommentIndexer();
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
          writeField(fieldDescriptor, new PathLocation(), 1);
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
            if (enumType.getFile() == fd) {
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

    private void writeField(Descriptors.FieldDescriptor field, PathLocation parent, int indent) {
      PathLocation location = parent.addField(field);
      writeLeadingComment(commentIndexer.getLocation(location), indent);
      indent(indent);
      writeFieldType(field);
      writer.print(" ");
      writer.print(field.getName());
      writer.print(" = ");
      writer.print(field.getNumber());

      writeOptionsForList(field.getOptions(), indent, "Field");
      writer.print(";");
      writeTrailingComment(commentIndexer.getLocation(location), indent);
    }

    private void writeOptionsForList(
        com.google.protobuf.GeneratedMessageV3.ExtendableMessage options,
        int indent,
        String optionType) {
      Map<Integer, Descriptors.FieldDescriptor> unknownMap;
      switch (optionType) {
        case "Field":
          unknownMap = domain.getOptions().getFieldOptionMap();
          break;
        case "EnumValue":
          unknownMap = domain.getOptions().getEnumValueOptionMap();
          break;
        default:
          throw new RuntimeException("Exception");
      }

      Map<Descriptors.FieldDescriptor, Object> resolved = new LinkedHashMap<>();
      resolved.putAll(options.getAllFields());
      resolved.putAll(convertUnknownFieldValue(options.getUnknownFields(), unknownMap));
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
              writeOptionForList(fieldDescriptor, o, indent, optionType);
              if (i < values.size() - 1) {
                writer.print(",");
              }
              writer.println();
            }
          } else {
            indent(indent + 1);
            writeOptionForList(fieldDescriptor, value, indent, optionType);
            if (iterator.hasNext()) {
              writer.print(",");
            }
            writer.println();
          }
        }
        indent(indent);
        writer.print("]");
      }
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

    private List<String> lines(String block) {
      List<String> lines = new ArrayList<>();
      for (String s : block.split("\n")) {
        lines.add(s);
      }
      return lines;
    }

    private void writeComment(String block, int indent) {
      lines(block)
          .forEach(
              line -> {
                indent(indent);
                writer.print("//");
                writer.println(line);
              });
    }

    private void writeLeadingComment(
        DescriptorProtos.SourceCodeInfo.Location location, int indent) {
      if (location != null) {
        location
            .getLeadingDetachedCommentsList()
            .forEach(
                block -> {
                  writeComment(block, indent);
                  writer.println();
                });
        writeComment(location.getLeadingComments(), indent);
      }
    }

    private void writeTrailingComment(
        DescriptorProtos.SourceCodeInfo.Location location, int indent) {
      if (location != null && location.hasTrailingComments()) {
        writer.println();
        writeComment(location.getTrailingComments(), indent);
      } else {
        writer.println();
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
      PathLocation location = new PathLocation();
      writeLeadingComment(commentIndexer.getSyntaxLocation(), 0);
      switch (fd.getSyntax()) {
        case PROTO2:
          writer.print("syntax = \"proto2\";");
          break;
        case PROTO3:
          writer.print("syntax = \"proto3\";");
          break;
        default:
          break;
      }
      writeTrailingComment(commentIndexer.getSyntaxLocation(), 0);
      writer.println();

      if (!fd.getPackage().isEmpty()) {
        writeLeadingComment(commentIndexer.getPackageLocation(), 0);
        writer.print("package ");
        writer.print(fd.getPackage());
        writer.print(";");
        writeTrailingComment(commentIndexer.getPackageLocation(), 0);
        writer.println();
      }

      List<Descriptors.FileDescriptor> dependencies = fd.getDependencies();
      if (dependencies.size() > 0) {
        int index = 0;
        for (Descriptors.FileDescriptor dependency : dependencies) {
          writeLeadingComment(commentIndexer.importLocations.get(index++), 0);
          writer.print("import \"");
          writer.print(dependency.getName());
          writer.print("\";");
          writeTrailingComment(commentIndexer.importLocations.get(index++), 0);
        }
        writer.println();
      }

      writeOptionsForBlock(fd.getOptions(), 0, "File");

      extensions();

      for (Descriptors.EnumDescriptor enumDescriptor : fd.getEnumTypes()) {
        writer.println();
        writeEnumDescriptor(enumDescriptor, location, 0);
      }

      for (Descriptors.ServiceDescriptor serviceDescriptor : fd.getServices()) {
        writer.println();
        writeServiceDescriptor(serviceDescriptor, location);
      }

      for (Descriptors.Descriptor messageType : fd.getMessageTypes()) {
        writer.println();
        writeMessageDescriptor(messageType, location, 0);
      }
    }

    private void writeServiceDescriptor(
        Descriptors.ServiceDescriptor serviceDescriptor, PathLocation parent) {
      PathLocation location = parent.addService(serviceDescriptor);
      writeLeadingComment(commentIndexer.getLocation(location), 0);
      writer.print("service ");
      writer.print(serviceDescriptor.getName());
      writer.println(" {");
      writeOptionsForBlock(serviceDescriptor.getOptions(), 1, "Service");
      for (Descriptors.MethodDescriptor method : serviceDescriptor.getMethods()) {
        PathLocation methodLocation = location.addMethod(method);
        writeLeadingComment(commentIndexer.getLocation(methodLocation), 1);
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
        if (options.getAllFields().size() == 0 && options.getUnknownFields().asMap().size() == 0) {
          writer.print(") {}");
        } else {
          writer.println(") {");
          writeOptionsForBlock(options, 2, "Method");
          indent(1);
          writer.print("}");
        }
        writeTrailingComment(commentIndexer.getLocation(methodLocation), 1);
      }
      writer.print("}");
      writeTrailingComment(commentIndexer.getLocation(location), 0);
    }

    private void writeEnumDescriptor(
        Descriptors.EnumDescriptor enumType, PathLocation parent, int indent) {
      PathLocation location = parent.addEnum(enumType);
      writeLeadingComment(commentIndexer.getLocation(location), indent);
      indent(indent);
      writer.print("enum ");
      writer.print(enumType.getName());
      writer.println(" {");
      writeOptionsForBlock(enumType.getOptions(), indent + 1, "Enum");
      for (Descriptors.EnumValueDescriptor value : enumType.getValues()) {
        indent(indent + 1);
        writer.print(value.getName());
        writer.print(" = ");
        writer.print(value.getNumber());
        writeOptionsForList(value.getOptions(), indent + 1, "EnumValue");
        writer.println(";");
      }
      indent(indent);
      writer.print("}");
      writeTrailingComment(commentIndexer.getLocation(location), indent);
    }

    private void writeMessageDescriptor(
        Descriptors.Descriptor messageType, PathLocation parent, int indent) {
      PathLocation location = parent.addMessage(messageType);
      writeLeadingComment(commentIndexer.getLocation(location), indent);
      indent(indent);
      writer.print("message ");
      writer.print(messageType.getName());
      writer.println(" {");

      writeOptionsForBlock(messageType.getOptions(), indent + 1, "Message");

      for (Descriptors.Descriptor nestedType : messageType.getNestedTypes()) {
        if (!nestedType.getOptions().getMapEntry()) {
          writeMessageDescriptor(nestedType, location, indent + 1);
          writer.println();
        }
      }
      for (Descriptors.EnumDescriptor enumType : messageType.getEnumTypes()) {
        writeEnumDescriptor(enumType, location, indent + 1);
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
            writeField(oneofField, location, indent + 2);
            ix++;
          }
          indent(indent + 1);
          writer.println("}");
        } else {
          writeField(field, location, indent + 1);
          ix++;
        }
      }

      indent(indent);
      writer.print("}");
      writeTrailingComment(commentIndexer.getLocation(location), indent);
    }

    private void writeOptionForBlock(
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
        Descriptors.FieldDescriptor fieldDescriptor, Object value, int indent, String optionType) {
      if (fieldDescriptor.getFullName().startsWith("google.protobuf." + optionType + "Options")) {
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
        String optionType) {

      Map<Integer, Descriptors.FieldDescriptor> unknownMap;
      switch (optionType) {
        case "File":
          unknownMap = domain.getOptions().getFileOptionMap();
          break;
        case "Message":
          unknownMap = domain.getOptions().getMessageOptionMap();
          break;
        case "Enum":
          unknownMap = domain.getOptions().getEnumOptionMap();
          break;
        case "Service":
          unknownMap = domain.getOptions().getServiceOptionMap();
          break;
        case "Method":
          unknownMap = domain.getOptions().getMethodOptionMap();
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
              values.forEach(v -> writeOptionForBlock(fieldDescriptor, v, indent, optionType));
            } else {
              writeOptionForBlock(fieldDescriptor, value, indent, optionType);
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

  private class PathLocation {

    private String path;

    public String toString() {
      return path;
    }

    private PathLocation() {
      this.path = "]";
    }

    private PathLocation(String path) {
      this.path = path;
    }

    PathLocation add(char t, int index) {
      return new PathLocation(path + "->" + t + "[" + index + "]");
    }

    PathLocation addMessage(Descriptors.Descriptor type) {
      return add('M', type.getIndex());
    }

    PathLocation addField(Descriptors.FieldDescriptor type) {
      return add('f', type.getIndex());
    }

    PathLocation addEnum(Descriptors.EnumDescriptor type) {
      return add('E', type.getIndex());
    }

    PathLocation addEnunValue(Descriptors.EnumValueDescriptor type) {
      return add('e', type.getIndex());
    }

    public PathLocation addService(Descriptors.ServiceDescriptor type) {
      return add('S', type.getIndex());
    }

    public PathLocation addMethod(Descriptors.MethodDescriptor type) {
      return add('m', type.getIndex());
    }
  }

  private class CommentIndexer {

    private DescriptorProtos.SourceCodeInfo.Location syntaxLocation;
    private DescriptorProtos.SourceCodeInfo.Location packageLocation;
    private Map<String, DescriptorProtos.SourceCodeInfo.Location> locations;
    private Map<Integer, DescriptorProtos.SourceCodeInfo.Location> importLocations;

    private boolean isBlank(String string) {
      return Strings.nullToEmpty(string).trim().isEmpty();
    }

    private CommentIndexer() {
      locations = new HashMap<>();
      importLocations = new HashMap<>();
      fd.toProto()
          .getSourceCodeInfo()
          .getLocationList()
          .forEach(
              location -> {
                boolean onlyWhitespace =
                    isBlank(location.getLeadingComments())
                        && isBlank(location.getTrailingComments())
                        && location.getLeadingDetachedCommentsCount() == 0;
                if (!onlyWhitespace) {
                  System.out.println("=====");
                  List<Integer> pathList = location.getPathList();
                  List<Integer> rest = new ArrayList();
                  if (location.getPathCount() > 2) {
                    rest = pathList.subList(2, pathList.size());
                  }
                  String p;
                  switch (location.getPath(0)) {
                    case 2:
                      packageLocation = location;
                      break;
                    case 3:
                      importLocations.put(location.getPath(1), location);
                      break;
                    case 4:
                      p =
                          pathForMessage(new PathLocation().add('M', location.getPath(1)), rest)
                              .toString();
                      System.out.println(p);
                      locations.put(p, location);
                      break;
                    case 5:
                      p =
                          pathForEnum(new PathLocation().add('E', location.getPath(1)), rest)
                              .toString();
                      System.out.println(p);
                      locations.put(p, location);
                      break;
                    case 6:
                      p =
                          pathForService(new PathLocation().add('S', location.getPath(1)), rest)
                              .toString();
                      System.out.println(p);
                      locations.put(p, location);
                      break;
                    case 12:
                      syntaxLocation = location;
                      break;
                    default:
                      System.out.println(location);
                      System.err.println("Unknown path type " + location.getPath(0));
                      throw new RuntimeException("Unknown path type " + location.getPath(0));
                  }
                }
              });
    }

    private PathLocation pathForMessage(PathLocation root, List<Integer> pathList) {
      if (pathList.size() == 0) {
        return root;
      }
      List<Integer> rest = new ArrayList();
      if (pathList.size() > 2) {
        rest = pathList.subList(2, pathList.size());
      }
      PathLocation pathLocation = null;
      switch (pathList.get(0)) {
        case 2:
          pathLocation = pathForMessage(root.add('f', pathList.get(1)), rest);
          break;
        case 3:
          pathLocation = pathForMessage(root.add('M', pathList.get(1)), rest);
          break;
        default:
          throw new RuntimeException("Unknown path type " + pathList.get(0));
      }
      return pathLocation;
    }

    private PathLocation pathForEnum(PathLocation root, List<Integer> pathList) {
      if (pathList.size() == 0) {
        return root;
      }
      List<Integer> rest = new ArrayList();
      if (pathList.size() > 2) {
        rest = pathList.subList(2, pathList.size());
      }
      PathLocation pathLocation = null;
      switch (pathList.get(0)) {
        case 2:
          pathLocation = pathForMessage(root.add('v', pathList.get(1)), rest);
          break;
        case 3:
          pathLocation = pathForMessage(root.add('O', pathList.get(1)), rest);
          break;
        default:
          throw new RuntimeException("Unknown path type " + pathList.get(0));
      }
      return pathLocation;
    }

    private PathLocation pathForService(PathLocation root, List<Integer> pathList) {
      if (pathList.size() == 0) {
        return root;
      }
      List<Integer> rest = new ArrayList();
      if (pathList.size() > 2) {
        rest = pathList.subList(2, pathList.size());
      }
      PathLocation pathLocation = null;
      switch (pathList.get(0)) {
        case 2:
          pathLocation = pathForMessage(root.add('m', pathList.get(1)), rest);
          break;
          //        case 3:
          //          pathLocation = pathForMessage(root.add('e', pathList.get(1)),rest);
          //          break;
        default:
          throw new RuntimeException("Unknown path type " + pathList.get(0));
      }
      return pathLocation;
    }

    public DescriptorProtos.SourceCodeInfo.Location getPackageLocation() {
      return packageLocation;
    }

    public DescriptorProtos.SourceCodeInfo.Location getSyntaxLocation() {
      return syntaxLocation;
    }

    public DescriptorProtos.SourceCodeInfo.Location getLocation(PathLocation location) {
      return locations.get(location.toString());
    }
  }
}
