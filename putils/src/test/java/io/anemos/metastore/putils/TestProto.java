package io.anemos.metastore.putils;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import test.v1.Option;

class TestProto {
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

  static final List<String> STING_LIST = new ArrayList<>();

  static {
    STING_LIST.add("Value I");
    STING_LIST.add("Value II");
    STING_LIST.add("Value III");
  }

  static final DescriptorProtos.FileOptions FILE_OPTIONS =
      DescriptorProtos.FileOptions.newBuilder()
          .setExtension(Option.fileOption, TEST_OPTION)
          .setExtension(Option.fileOption1, 12)
          .setExtension(Option.fileOption2, "String")
          .setExtension(Option.fileOptionN, STING_LIST)
          .setDeprecated(true)
          .build();

  static final DescriptorProtos.MessageOptions MESSAGE_OPTIONS =
      DescriptorProtos.MessageOptions.newBuilder()
          .setExtension(Option.messageOption, TEST_OPTION)
          .setExtension(Option.messageOption1, 12)
          .setExtension(Option.messageOption2, "String")
          .setExtension(Option.messageOptionN, STING_LIST)
          .setDeprecated(true)
          .build();

  static final DescriptorProtos.FieldOptions FIELD_OPTIONS =
      DescriptorProtos.FieldOptions.newBuilder()
          .setExtension(Option.fieldOption, TEST_OPTION)
          .setExtension(Option.fieldOption1, 12)
          .setExtension(Option.fieldOption2, "String")
          .setExtension(Option.fieldOptionN, STING_LIST)
          .setDeprecated(true)
          .build();

  static final DescriptorProtos.ServiceOptions SERVICE_OPTIONS =
      DescriptorProtos.ServiceOptions.newBuilder()
          .setExtension(Option.serviceOption, TEST_OPTION)
          .setExtension(Option.serviceOption1, 12)
          .setExtension(Option.serviceOption2, "String")
          .setExtension(Option.serviceOptionN, STING_LIST)
          .setDeprecated(true)
          .build();

  static final DescriptorProtos.MethodOptions METHOD_OPTIONS =
      DescriptorProtos.MethodOptions.newBuilder()
          .setExtension(Option.methodOption, TEST_OPTION)
          .setExtension(Option.methodOption1, 12)
          .setExtension(Option.methodOption2, "String")
          .setExtension(Option.methodOptionN, STING_LIST)
          .setDeprecated(true)
          .build();

  static final DescriptorProtos.EnumOptions ENUM_OPTIONS =
      DescriptorProtos.EnumOptions.newBuilder()
          .setExtension(Option.enumOption, TEST_OPTION)
          .setExtension(Option.enumOption1, 12)
          .setExtension(Option.enumOption2, "String")
          .setExtension(Option.enumOptionN, STING_LIST)
          .setDeprecated(true)
          .build();

  static final DescriptorProtos.EnumValueOptions ENUM_VALUE_OPTIONS =
      DescriptorProtos.EnumValueOptions.newBuilder()
          .setExtension(Option.enumValueOption, TEST_OPTION)
          .setExtension(Option.enumValueOption1, 12)
          .setExtension(Option.enumValueOption2, "String")
          .setExtension(Option.enumValueOptionN, STING_LIST)
          .setDeprecated(true)
          .build();

  static final DescriptorProtos.FileDescriptorProto FILE_DESCRIPTOR_PROTO1 =
      DescriptorProtos.FileDescriptorProto.newBuilder()
          .setName("x/y/z/f1.proto")
          .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("F1M1").build())
          .build();

  static final DescriptorProtos.FileDescriptorProto FILE_DESCRIPTOR_PROTO2 =
      DescriptorProtos.FileDescriptorProto.newBuilder()
          .setName("x/y/z/f2.proto")
          .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("F2M1").build())
          .build();

  static final DescriptorProtos.FileDescriptorProto FILE_DESCRIPTOR_PROTO3 =
      DescriptorProtos.FileDescriptorProto.newBuilder()
          .setName("x/y/z/f3.proto")
          .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("F3M1").build())
          .build();

  static final Collection<ByteString> FILE_DESCRIPTOR_LIST = new ArrayList<>();

  static {
    FILE_DESCRIPTOR_LIST.add(FILE_DESCRIPTOR_PROTO1.toByteString());
    FILE_DESCRIPTOR_LIST.add(FILE_DESCRIPTOR_PROTO2.toByteString());
  }
}
