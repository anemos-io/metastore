package io.anemos.metastore.core.proto;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import test.v1.Option;

@RunWith(JUnit4.class)
public class ProtoLanguageFileWriterTest {

  static final Option.TestOption TEST_MINIMAL =
      Option.TestOption.newBuilder()
          .setSingleString("minimal")
          .addRepeatedString("test1")
          .addRepeatedString("test2")
          .setSingleInt32(2)
          .addRepeatedInt32(3)
          .setSingleEnum(Option.TestOption.TestEnum.ENUM2)
          .build();

  static final Option.TestOption TEST_OPTION =
      Option.TestOption.newBuilder()
          .setSingleString("testString")
          .addRepeatedString("test1")
          .addRepeatedString("test2")
          .setSingleInt32(2)
          .addRepeatedInt32(3)
          .addRepeatedInt32(4)
          .setSingleInt64(10)
          //              .setSingleBytes(ByteString.copyFrom(new byte[] { 0x00, 0x01, 0x02 }))
          .setSingleEnum(Option.TestOption.TestEnum.ENUM2)
          .setSingleMessage(TEST_MINIMAL)
          .build();

  static final List STING_LIST = new ArrayList<String>();

  static {
    STING_LIST.add("Value I");
    STING_LIST.add("Value II");
    STING_LIST.add("Value III");
  }

  private void testOutput(
      DescriptorProtos.FileDescriptorProto.Builder protoBuilder,
      PContainer container,
      String expected)
      throws Descriptors.DescriptorValidationException {
    Descriptors.FileDescriptor[] dependencies = new Descriptors.FileDescriptor[1];
    dependencies[0] = Option.getDescriptor();
    Descriptors.FileDescriptor fileDescriptor =
        Descriptors.FileDescriptor.buildFrom(protoBuilder.build(), dependencies);

    testOutput(fileDescriptor, container, expected);
  }

  private void testOutput(
      Descriptors.FileDescriptor fileDescriptor, PContainer container, String expected)
      throws Descriptors.DescriptorValidationException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ProtoLanguageFileWriter.write(fileDescriptor, container, outputStream);
    Assert.assertEquals(expected, outputStream.toString());
  }

  @Test
  public void noPackageSetTest() throws Exception {
    DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder =
        DescriptorProtos.FileDescriptorProto.newBuilder().setName("test").setSyntax("proto3");

    DescriptorProtos.DescriptorProto.Builder descriptor =
        DescriptorProtos.DescriptorProto.newBuilder();
    descriptor.setName("TestMessage");
    fileDescriptorProtoBuilder.addMessageType(descriptor);

    testOutput(
        fileDescriptorProtoBuilder,
        null,
        "syntax = \"proto3\";\n"
            + "\n"
            + "import \"test/v1/option.proto\";\n"
            + "\n"
            + "\n"
            + "\n"
            + "message TestMessage {\n"
            + "\n"
            + "}\n");
  }

  @Test
  public void fieldOptionsTest() throws Exception {
    DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder =
        DescriptorProtos.FileDescriptorProto.newBuilder().setName("test").setSyntax("proto3");

    DescriptorProtos.DescriptorProto.Builder descriptor =
        DescriptorProtos.DescriptorProto.newBuilder();
    descriptor.setName("TestMessage");

    DescriptorProtos.FieldDescriptorProto.Builder fieldDescriptorProtoBuilder =
        DescriptorProtos.FieldDescriptorProto.newBuilder()
            .setName("string")
            .setNumber(123)
            .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)
            .setOptions(
                DescriptorProtos.FieldOptions.newBuilder()
                    .setExtension(Option.fieldOption1, 123)
                    .setExtension(Option.fieldOption2, "something")
                    .build());

    DescriptorProtos.FieldDescriptorProto.Builder fieldDescriptorProtoBuilder2 =
        DescriptorProtos.FieldDescriptorProto.newBuilder()
            .setName("field2")
            .setNumber(124)
            .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING);

    descriptor.addField(fieldDescriptorProtoBuilder);
    descriptor.addField(fieldDescriptorProtoBuilder2);
    fileDescriptorProtoBuilder.addMessageType(descriptor);

    ArrayList<Descriptors.FileDescriptor> dependencies = new ArrayList<>();
    dependencies.add(Option.getDescriptor());
    Descriptors.FileDescriptor[] list = new Descriptors.FileDescriptor[dependencies.size()];
    dependencies.toArray(list);
    Descriptors.FileDescriptor fileDescriptor =
        Descriptors.FileDescriptor.buildFrom(fileDescriptorProtoBuilder.build(), list);

    testOutput(
        fileDescriptor,
        null,
        "syntax = \"proto3\";\n"
            + "\n"
            + "import \"test/v1/option.proto\";\n"
            + "\n"
            + "\n"
            + "\n"
            + "message TestMessage {\n"
            + "\n"
            + "\tstring string = 123 [\n"
            + "\t\t(test.v1.field_option_1) = 123, \n"
            + "\t\t(test.v1.field_option_2) = \"something\"\n"
            + "\t];\n"
            + "\n"
            + "\tstring field2 = 124;\n"
            + "}\n");
  }

  @Test
  public void extensionTest() throws Exception {
    DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder =
        DescriptorProtos.FileDescriptorProto.newBuilder()
            .setName("test")
            .setSyntax("proto3")
            .addDependency("google/protobuf/descriptor.proto");

    DescriptorProtos.FieldDescriptorProto extensionField =
        DescriptorProtos.FieldDescriptorProto.newBuilder()
            .setName("test_extension")
            .setNumber(66666700)
            .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)
            .setExtendee(".google.protobuf.FileOptions")
            .build();

    fileDescriptorProtoBuilder.addExtension(extensionField);

    Descriptors.FileDescriptor[] dependencies = new Descriptors.FileDescriptor[1];
    dependencies[0] = DescriptorProtos.getDescriptor();
    Descriptors.FileDescriptor fileDescriptor =
        Descriptors.FileDescriptor.buildFrom(fileDescriptorProtoBuilder.build(), dependencies);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ProtoLanguageFileWriter.write(fileDescriptor, outputStream);

    String expected =
        "syntax = \"proto3\";\n"
            + "\n"
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "\n"
            + "\n"
            + "\n"
            + "extend google.protobuf.FileOptions {\n"
            + "\tstring test_extension = 66666700;\n"
            + "}\n"
            + "\n";
    Assert.assertEquals(expected, outputStream.toString());
  }

  @Test
  public void unknownMessageOptionsTest() throws Exception {
    PContainer PContainer = TestSets.baseComplexMessageOptions();
    Descriptors.FileDescriptor fileDescriptor =
        PContainer.getFileDescriptorByFileName("test/v1/complex.proto");

    testOutput(
        fileDescriptor,
        PContainer,
        "syntax = \"proto3\";\n"
            + "\n"
            + "import \"test/v1/option.proto\";\n"
            + "\n"
            + "option (test_file_option) = {\n"
            + "\tstring: \"test\",\n"
            + "\trepeated_string: [\"test1\",\"test2\"],\n"
            + "\tint32: 42,\n"
            + "\trepeated_int32: [1,2],\n"
            + "\tint64: 43\n"
            + "};\n"
            + "option (string_file_option) = \"test\";\n"
            + "option (repeated_string_file_option) = \"test1\";\n"
            + "option (repeated_string_file_option) = \"test2\";\n"
            + "option (int32_file_option) = 42;\n"
            + "option (repeated_int32_file_option) = 42;\n"
            + "option (repeated_int32_file_option) = 43;\n"
            + "option (int64_file_option) = 42;\n"
            + "option (repeated_int64_file_option) = 42;\n"
            + "option (repeated_int64_file_option) = 43;\n"
            + "option (bool_file_option) = true;\n"
            + "option (double_file_option) = 3.14;\n"
            + "option (float_file_option) = 3.14;\n"
            + "\n"
            + "package test.v1;\n"
            + "\n"
            + "message ProtoBeamBasicMessage {\n"
            + "\toption (test_option) = {\n"
            + "\t\tstring: \"test\",\n"
            + "\t\trepeated_string: [\"test1\",\"test2\"],\n"
            + "\t\tint32: 42,\n"
            + "\t\trepeated_int32: [1,2],\n"
            + "\t\tint64: 43\n"
            + "\t};\n"
            + "\toption (string_option) = \"test\";\n"
            + "\toption (repeated_string_option) = \"test1\";\n"
            + "\toption (repeated_string_option) = \"test2\";\n"
            + "\toption (int32_option) = 42;\n"
            + "\toption (repeated_int32_option) = 42;\n"
            + "\toption (repeated_int32_option) = 43;\n"
            + "\toption (int64_option) = 42;\n"
            + "\toption (repeated_int64_option) = 42;\n"
            + "\toption (repeated_int64_option) = 43;\n"
            + "\toption (bool_option) = true;\n"
            + "\toption (double_option) = 3.14;\n"
            + "\toption (float_option) = 3.14;\n"
            + "\n"
            + "\tstring test_name = 1;\n"
            + "\tint32 test_index = 2 [\n"
            + "\t\t(test_field_option) = {\n"
            + "\t\t\tstring: \"test\",\n"
            + "\t\t\trepeated_string: [\"test1\",\"test2\"],\n"
            + "\t\t\tint32: 42,\n"
            + "\t\t\trepeated_int32: [1,2],\n"
            + "\t\t\tint64: 43\n"
            + "\t\t},\n"
            + "\t\t(string_field_option) = \"test\",\n"
            + "\t\t(repeated_string_field_option) = \"test1\",\n"
            + "\t\t(repeated_string_field_option) = \"test2\",\n"
            + "\t\t(int32_field_option) = 42,\n"
            + "\t\t(repeated_int32_field_option) = 42,\n"
            + "\t\t(repeated_int32_field_option) = 43,\n"
            + "\t\t(int64_field_option) = 42,\n"
            + "\t\t(repeated_int64_field_option) = 42,\n"
            + "\t\t(repeated_int64_field_option) = 43,\n"
            + "\t\t(bool_field_option) = true,\n"
            + "\t\t(double_field_option) = 3.14,\n"
            + "\t\t(float_field_option) = 3.14\n"
            + "\t];\n"
            + "\n"
            + "\tdouble primitive_double = 3;\n"
            + "\tfloat primitive_float = 4;\n"
            + "\tint32 primitive_int32 = 5;\n"
            + "\tint64 primitive_int64 = 6;\n"
            + "\tuint32 primitive_uint32 = 7;\n"
            + "\tuint64 primitive_uint64 = 8;\n"
            + "\tsint32 primitive_sint32 = 9;\n"
            + "\tsint64 primitive_sint64 = 10;\n"
            + "\tfixed32 primitive_fixed32 = 11;\n"
            + "\tfixed64 primitive_fixed64 = 12;\n"
            + "\tsfixed32 primitive_sfixed32 = 13;\n"
            + "\tsfixed64 primitive_sfixed64 = 14;\n"
            + "\tbool primitive_bool = 15;\n"
            + "\tstring primitive_string = 16;\n"
            + "\tbytes primitive_bytes = 17;\n"
            + "}\n");
  }

  @Test
  public void writeMessage() throws Exception {
    DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder =
        DescriptorProtos.FileDescriptorProto.newBuilder().setName("test").setSyntax("proto3");

    DescriptorProtos.DescriptorProto.Builder descriptor =
        DescriptorProtos.DescriptorProto.newBuilder();
    descriptor.setName("TestMessage");

    DescriptorProtos.MessageOptions messageOptions =
        DescriptorProtos.MessageOptions.newBuilder()
            .setExtension(Option.messageOption, TEST_OPTION)
            .build();

    descriptor.setOptions(messageOptions);
    fileDescriptorProtoBuilder.addMessageType(descriptor);

    testOutput(
        fileDescriptorProtoBuilder,
        null,
        "syntax = \"proto3\";\n"
            + "\n"
            + "import \"test/v1/option.proto\";\n"
            + "\n"
            + "\n"
            + "\n"
            + "message TestMessage {\n"
            + "\toption test.v1.message_option.(single_string) = \"testString\";\n"
            + "\toption test.v1.message_option.(repeated_string) = \"test1\";\n"
            + "\toption test.v1.message_option.(repeated_string) = \"test2\";\n"
            + "\toption test.v1.message_option.(single_int32) = 2;\n"
            + "\toption test.v1.message_option.(repeated_int32) = 3;\n"
            + "\toption test.v1.message_option.(repeated_int32) = 4;\n"
            + "\toption test.v1.message_option.(single_int64) = 10;\n"
            + "\toption test.v1.message_option.(single_enum) = ENUM2;\n"
            + "\n"
            + "}\n");
  }

  @Test
  public void writeService() throws Exception {
    DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder =
        DescriptorProtos.FileDescriptorProto.newBuilder()
            .setName("test")
            .setSyntax("proto3")
            .addDependency("google/protobuf/descriptor.proto");

    DescriptorProtos.DescriptorProto methodRequest =
        DescriptorProtos.DescriptorProto.newBuilder().setName("MethodRequest").build();

    DescriptorProtos.DescriptorProto methodResponse =
        DescriptorProtos.DescriptorProto.newBuilder().setName("MethodResponse").build();

    DescriptorProtos.ServiceDescriptorProto service =
        DescriptorProtos.ServiceDescriptorProto.newBuilder()
            .setName("Service")
            .addMethod(
                DescriptorProtos.MethodDescriptorProto.newBuilder()
                    .setName("FirstMethod")
                    .setInputType("MethodRequest")
                    .setOutputType("MethodResponse"))
            .addMethod(
                DescriptorProtos.MethodDescriptorProto.newBuilder()
                    .setName("ClientStreamingMethod")
                    .setInputType("MethodRequest")
                    .setOutputType("MethodResponse")
                    .setClientStreaming(true))
            .addMethod(
                DescriptorProtos.MethodDescriptorProto.newBuilder()
                    .setName("ServerStreamingMethod")
                    .setInputType("MethodRequest")
                    .setOutputType("MethodResponse")
                    .setServerStreaming(true)
                    .setOptions(
                        DescriptorProtos.MethodOptions.newBuilder()
                            .setExtension(Option.methodOption, TEST_MINIMAL)))
            .addMethod(
                DescriptorProtos.MethodDescriptorProto.newBuilder()
                    .setName("BiStreamingMethod")
                    .setInputType("MethodRequest")
                    .setOutputType("MethodResponse")
                    .setClientStreaming(true)
                    .setServerStreaming(true)
                    .setOptions(
                        DescriptorProtos.MethodOptions.newBuilder()
                            .setExtension(Option.methodOption, TEST_OPTION)
                            .setExtension(Option.methodOption1, 12)
                            .setExtension(Option.methodOption2, "String")
                            .setExtension(Option.methodOptionN, STING_LIST)
                            .build()))
            .setOptions(
                DescriptorProtos.ServiceOptions.newBuilder()
                    .setExtension(Option.serviceOption, TEST_OPTION)
                    .build())
            .build();

    fileDescriptorProtoBuilder.addService(service);
    fileDescriptorProtoBuilder.addMessageType(methodRequest);
    fileDescriptorProtoBuilder.addMessageType(methodResponse);

    testOutput(
        fileDescriptorProtoBuilder,
        null,
        "syntax = \"proto3\";\n"
            + "\n"
            + "import \"test/v1/option.proto\";\n"
            + "\n"
            + "\n"
            + "\n"
            + "service Service {\n"
            + "\trpc FirstMethod(MethodRequest) returns (MethodResponse) {}\n"
            + "\trpc ClientStreamingMethod(stream MethodRequest) returns (MethodResponse) {}\n"
            + "\trpc ServerStreamingMethod(MethodRequest) returns (stream MethodResponse) {\n"
            + "\t\toption (test.v1.method_option) = {\n"
            + "\t\t\tsingle_string: \"minimal\"\n"
            + "\t\t\trepeated_string: [\"test1\",\"test2\"]\n"
            + "\t\t\tsingle_int32: 2\n"
            + "\t\t\trepeated_int32: [3]\n"
            + "\t\t\tsingle_enum: ENUM2\n"
            + "\t\t};\n"
            + "\n"
            + "\t}\n"
            + "\trpc BiStreamingMethod(stream MethodRequest) returns (stream MethodResponse) {\n"
            + "\t\toption (test.v1.method_option) = {\n"
            + "\t\t\tsingle_string: \"testString\"\n"
            + "\t\t\trepeated_string: [\"test1\",\"test2\"]\n"
            + "\t\t\tsingle_int32: 2\n"
            + "\t\t\trepeated_int32: [3,4]\n"
            + "\t\t\tsingle_int64: 10\n"
            + "\t\t\tsingle_enum: ENUM2\n"
            + "\t\t\tsingle_message: {\n"
            + "\t\t\t\tsingle_string: \"minimal\"\n"
            + "\t\t\t\trepeated_string: [\"test1\",\"test2\"]\n"
            + "\t\t\t\tsingle_int32: 2\n"
            + "\t\t\t\trepeated_int32: [3]\n"
            + "\t\t\t\tsingle_enum: ENUM2\n"
            + "\t\t\t};\n"
            + "\n"
            + "\t\t};\n"
            + "\t\toption (test.v1.method_option_1) = 12;\n"
            + "\t\toption (test.v1.method_option_2) = \"String\";\n"
            + "\t\toption (test.v1.method_option_n) = \"Value I\";\n"
            + "\t\toption (test.v1.method_option_n) = \"Value II\";\n"
            + "\t\toption (test.v1.method_option_n) = \"Value III\";\n"
            + "\n"
            + "\t}\n"
            + "}\n"
            + "\n"
            + "message MethodRequest {\n"
            + "\n"
            + "}\n"
            + "\n"
            + "message MethodResponse {\n"
            + "\n"
            + "}\n");
  }
}
