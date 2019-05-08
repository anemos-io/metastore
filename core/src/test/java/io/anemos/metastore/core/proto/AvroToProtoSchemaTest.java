package io.anemos.metastore.core.proto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
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
            getProtoDescriptor(new AvroToProtoSchema(node).toDescriptor()),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestSingleInt");
    Assert.assertEquals(0, result.getReport().getMessageResultsCount());
  }

  @Test
  public void testSingleBoolean() throws IOException {
    JsonNode node = getJsonNode("testSingleBoolean");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestSingleBoolean.getDescriptor()),
            getProtoDescriptor(new AvroToProtoSchema(node).toDescriptor()),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestSingleBoolean");
    Assert.assertEquals(0, result.getReport().getMessageResultsCount());
  }

  @Test
  public void testSingleLong() throws IOException {
    JsonNode node = getJsonNode("testSingleLong");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestSingleLong.getDescriptor()),
            getProtoDescriptor(new AvroToProtoSchema(node).toDescriptor()),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestSingleLong");
    Assert.assertEquals(0, result.getReport().getMessageResultsCount());
  }

  @Test
  public void testSingleFloat() throws IOException {
    JsonNode node = getJsonNode("testSingleFloat");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestSingleFloat.getDescriptor()),
            getProtoDescriptor(new AvroToProtoSchema(node).toDescriptor()),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestSingleFloat");
    Assert.assertEquals(0, result.getReport().getMessageResultsCount());
  }

  @Test
  public void testSingleDouble() throws IOException {
    JsonNode node = getJsonNode("testSingleDouble");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestSingleDouble.getDescriptor()),
            getProtoDescriptor(new AvroToProtoSchema(node).toDescriptor()),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestSingleDouble");
    Assert.assertEquals(0, result.getReport().getMessageResultsCount());
  }

  @Test
  public void testSingleBytes() throws IOException {
    JsonNode node = getJsonNode("testSingleBytes");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestSingleBytes.getDescriptor()),
            getProtoDescriptor(new AvroToProtoSchema(node).toDescriptor()),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestSingleBytes");
    Assert.assertEquals(0, result.getReport().getMessageResultsCount());
  }

  @Test
  public void testSingleString() throws IOException {
    JsonNode node = getJsonNode("testSingleString");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestSingleString.getDescriptor()),
            getProtoDescriptor(new AvroToProtoSchema(node).toDescriptor()),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestSingleString");
    Assert.assertEquals(0, result.getReport().getMessageResultsCount());
  }

  @Test
  public void testComplexArray() throws IOException {
    JsonNode node = getJsonNode("testComplexArray");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestComplexArrayInt.getDescriptor()),
            getProtoDescriptor(new AvroToProtoSchema(node).toDescriptor()),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestComplexArrayInt");
    Assert.assertEquals(0, result.getReport().getMessageResultsCount());
  }

  @Test
  public void testComplexEnum() throws IOException {
    JsonNode node = getJsonNode("testComplexEnum");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestComplexEnum.getDescriptor()),
            getProtoDescriptor(new AvroToProtoSchema(node).toDescriptor()),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestComplexEnum");
    Assert.assertEquals(0, result.getReport().getMessageResultsCount());
  }

  @Test
  @Ignore
  public void testComplexMap() throws IOException {
    JsonNode node = getJsonNode("testComplexMap");
    ValidationResults result = new ValidationResults();
    new ProtoDiff(
            getProtoDescriptor(TestComplexEnum.getDescriptor()),
            getProtoDescriptor(new AvroToProtoSchema(node).toDescriptor()),
            result)
        .diffOnMessage("io.anemos.metastore.core.proto.TestComplexMap");
    Assert.assertEquals(0, result.getReport().getMessageResultsCount());
  }

  private JsonNode getJsonNode(String jsonName) {
    InputStream resourceAsStream =
        AvroToProtoSchemaTest.class.getResourceAsStream(jsonName.concat(".json"));
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = null;
    try {
      node = mapper.readTree(resourceAsStream);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return node;
  }

  private ProtoDescriptor getProtoDescriptor(Object input) throws IOException {
    if (input instanceof DescriptorProtos.FileDescriptorProto) {
      DescriptorProtos.FileDescriptorProto descriptorNew =
          (DescriptorProtos.FileDescriptorProto) input;
      DescriptorProtos.FileDescriptorSet theSet =
          DescriptorProtos.FileDescriptorSet.newBuilder().addFile(descriptorNew).build();
      return new ProtoDescriptor(theSet.toByteArray());
    } else if (input instanceof Descriptors.Descriptor) {
      Descriptors.Descriptor descriptorRef = (Descriptors.Descriptor) input;
      return new ProtoDescriptor(descriptorRef);
    }
    return null;
  }
}
