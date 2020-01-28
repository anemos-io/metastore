package io.anemos.metastore.core.proto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Descriptors;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.putils.ProtoDomain;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AvroToProtoSchemaTest {

  @Test
  public void testSingleInt() throws IOException {
    JsonNode node = getJsonNode("testSingleInt");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestSingleInt.getDescriptor()),
            new AvroToProtoSchema(node).get(),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestSingleInt");
    Assert.assertEquals(0, result.getPatch().getMessagePatchesCount());
  }

  @Test
  public void testSingleBoolean() throws IOException {
    JsonNode node = getJsonNode("testSingleBoolean");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestSingleBoolean.getDescriptor()),
            new AvroToProtoSchema(node).get(),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestSingleBoolean");
    Assert.assertEquals(0, result.getPatch().getMessagePatchesCount());
  }

  @Test
  public void testSingleLong() throws IOException {
    JsonNode node = getJsonNode("testSingleLong");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestSingleLong.getDescriptor()),
            new AvroToProtoSchema(node).get(),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestSingleLong");
    Assert.assertEquals(0, result.getPatch().getMessagePatchesCount());
  }

  @Test
  public void testSingleFloat() throws IOException {
    JsonNode node = getJsonNode("testSingleFloat");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestSingleFloat.getDescriptor()),
            new AvroToProtoSchema(node).get(),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestSingleFloat");
    Assert.assertEquals(0, result.getPatch().getMessagePatchesCount());
  }

  @Test
  public void testSingleDouble() throws IOException {
    JsonNode node = getJsonNode("testSingleDouble");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestSingleDouble.getDescriptor()),
            new AvroToProtoSchema(node).get(),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestSingleDouble");
    Assert.assertEquals(0, result.getPatch().getMessagePatchesCount());
  }

  @Test
  public void testSingleBytes() throws IOException {
    JsonNode node = getJsonNode("testSingleBytes");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestSingleBytes.getDescriptor()),
            new AvroToProtoSchema(node).get(),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestSingleBytes");
    Assert.assertEquals(0, result.getPatch().getMessagePatchesCount());
  }

  @Test
  public void testSingleString() throws IOException {
    JsonNode node = getJsonNode("testSingleString");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestSingleString.getDescriptor()),
            new AvroToProtoSchema(node).get(),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestSingleString");
    Assert.assertEquals(0, result.getPatch().getMessagePatchesCount());
  }

  @Test
  public void testComplexArray() throws IOException {
    JsonNode node = getJsonNode("testComplexArray");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestComplexArrayInt.getDescriptor()),
            new AvroToProtoSchema(node).get(),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestComplexArrayInt");
    Assert.assertEquals(0, result.getPatch().getMessagePatchesCount());
  }

  @Test
  public void testComplexEnum() throws IOException {
    JsonNode node = getJsonNode("testComplexEnum");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestComplexEnum.getDescriptor()),
            new AvroToProtoSchema(node).get(),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestComplexEnum");
    Assert.assertEquals(0, result.getPatch().getMessagePatchesCount());
  }

  @Test
  public void testDebeziumExample1() throws IOException {
    JsonNode node = getJsonNode("testDebeziumExample1");
    ProtoDomain descriptor = new AvroToProtoSchema(node).get();
    System.out.println(descriptor);
  }

  @Test
  @Ignore
  public void testComplexMap() throws IOException {
    JsonNode node = getJsonNode("testComplexMap");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestComplexEnum.getDescriptor()),
            new AvroToProtoSchema(node).get(),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestComplexMap");
    Assert.assertEquals(0, result.getPatch().getMessagePatchesCount());
  }

  private JsonNode getJsonNode(String jsonName) {
    InputStream resourceAsStream =
        AvroToProtoSchemaTest.class.getResourceAsStream("../".concat(jsonName.concat(".json")));
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = null;
    try {
      node = mapper.readTree(resourceAsStream);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return node;
  }

  private ProtoDomain getProtoDescriptor(Descriptors.Descriptor input) throws IOException {
    return ProtoDomain.buildFrom(input);
  }

  //    if (input instanceof DescriptorProtos.FileDescriptorProto) {
  //      DescriptorProtos.FileDescriptorProto descriptorNew =
  //          (DescriptorProtos.FileDescriptorProto) input;
  //      DescriptorProtos.FileDescriptorSet theSet =
  //          DescriptorProtos.FileDescriptorSet.newBuilder().addFile(descriptorNew).build();
  //      return new PContainer(theSet.toByteArray());
  //    } else if (input instanceof Descriptors.Descriptor) {
  //      Descriptors.Descriptor descriptorRef = (Descriptors.Descriptor) input;
  //      return new PContainer(descriptorRef);
  //    }
  //    return null;

}
