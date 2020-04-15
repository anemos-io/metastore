package io.anemos.metastore.core.proto;

import acme.jumpgate.v1alpha1.TestComplexJsonschema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.DescriptorProtos;
import io.anemos.metastore.putils.ProtoDomain;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Assert;
import org.junit.Test;
import test.v1.Option;

public class ProtoToAvroSchemaTest {
  @Test
  public void testSingleString() throws IOException {
    String node = getJsonNode("testSingleString");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestSingleString.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestSingleString", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testSingleInt() throws IOException {
    String node = getJsonNode("testSingleInt");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestSingleInt.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestSingleInt", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testSingleLong() throws IOException {
    String node = getJsonNode("testSingleLong");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestSingleLong.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestSingleLong", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testSingleFloat() throws IOException {
    String node = getJsonNode("testSingleFloat");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestSingleFloat.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestSingleFloat", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testSingleDouble() throws IOException {
    String node = getJsonNode("testSingleDouble");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestSingleDouble.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestSingleDouble", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testSingleBoolean() throws IOException {
    String node = getJsonNode("testSingleBoolean");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestSingleBoolean.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestSingleBoolean", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testSingleBytes() throws IOException {
    String node = getJsonNode("testSingleBytes");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestSingleBytes.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestSingleBytes", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testSingleTimestamp() throws IOException {
    String node = getJsonNode("testSingleTimestamp");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestSingleTimestamp.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestSingleTimestamp", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void TestComplexEnum() throws IOException {
    String node = getJsonNode("testComplexEnum");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestComplexEnum.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestComplexEnum", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testComplexExtEnum() throws IOException {
    String node = getJsonNode("testComplexExtEnum");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestComplexExtEnum.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestComplexExtEnum", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testComplexTopEnum() throws IOException {
    String node = getJsonNode("testComplexTopEnum");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestComplexTopEnum.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestComplexTopEnum", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testComplexArrayInt() throws IOException {
    String node = getJsonNode("testComplexArray");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestComplexArrayInt.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestComplexArrayInt", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testComplexWrapperValues() throws IOException {
    String node = getJsonNode("testComplexWrapperValues");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestWrapperTypes.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestWrapperTypes", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testComplexMap() throws IOException {
    String node = getJsonNode("testComplexMap");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestComplexMap.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestComplexMap", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testComplexNestedMessage() throws IOException {
    String node = getJsonNode("testComplexNestedMessage");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestSingleNested.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.TestSingleNested", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testJumpMessage() throws IOException {
    String node = getJsonNode("testComplexJumpMessage");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestComplexJsonschema.Jump.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain, String.format("%s.Jump", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testRepeatedMessage() throws IOException {
    String node = getJsonNode("testRepeatedMessage");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestMultipleRepeated.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain = ProtoDomain.builder().add(fileDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain,
            String.format("%s.TestMultipleRepeated", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  @Test
  public void testMessageWithOptions() throws IOException {
    String node = getJsonNode("testComplexWithOptions");

    final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestComplexWithOptions.getDescriptor().getFile().toProto();
    final DescriptorProtos.FileDescriptorProto optionsDescriptorProto =
        Option.TestOption.getDescriptor().getFile().toProto();
    ProtoDomain protoDomain =
        ProtoDomain.builder().add(fileDescriptorProto).add(optionsDescriptorProto).build();

    String avroSchema =
        ProtoToAvroSchema.convert(
            protoDomain,
            String.format("%s.TestComplexWithOptions", fileDescriptorProto.getPackage()));

    Assert.assertEquals(node, avroSchema);
  }

  private String getJsonNode(String jsonName) {
    InputStream resourceAsStream =
        AvroToProtoSchemaTest.class.getResourceAsStream("../".concat(jsonName.concat(".json")));
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = null;
    try {
      node = mapper.readTree(resourceAsStream);
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}
