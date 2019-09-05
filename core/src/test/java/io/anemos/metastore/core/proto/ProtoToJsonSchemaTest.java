package io.anemos.metastore.core.proto;

import org.junit.Assert;
import org.junit.Test;

public class ProtoToJsonSchemaTest {
  @Test
  public void testMinMaxValue() {
    System.out.println();
  }

  @Test
  public void testSingleLong() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleLong.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleLong");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleLong\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"integer\",\"minimum\":-9223372036854775808,\"maximum\":9223372036854775807}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleInt() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleInt.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleInt");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleInt\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"integer\",\"minimum\":-2147483648,\"maximum\":2147483647}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleUInt() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleUInt.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleUInt");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleUInt\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"integer\",\"minimum\":0,\"maximum\":4294967295}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleBoolean() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleBoolean.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleBoolean");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleBoolean\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"boolean\"}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleFloat() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleFloat.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleFloat");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleFloat\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"numeric\",\"minimum\":1.401298464324817E-45,\"maximum\":1.401298464324817E-45}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleDouble() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleDouble.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleDouble");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleDouble\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"numeric\",\"minimum\":4.9E-324,\"maximum\":4.9E-324}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleBytes() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleBytes.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleBytes");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleBytes\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"string\",\"contentEncoding\":\"base64\"}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleString() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleString.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleString");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleString\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"string\"}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleUInt64() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleUInt64.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleUInt64");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleUInt64\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"integer\",\"minimum\":0,\"maximum\":123456789012345678}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleEnum() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleEnum.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleEnum");

    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleEnum\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"string\",\"enum\":[\"ENUM1\",\"ENUM2\",\"ENUM3\"]}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleExtEnum() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleExtEnum.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleExtEnum");

    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleExtEnum\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"string\",\"enum\":[\"ENUMVAL1\",\"ENUMVAL2\"]}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleTopEnum() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleTopEnum.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleTopEnum");

    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleTopEnum\",\"type\":\"object\",\"properties\":{\"billing\":{\"type\":\"string\",\"enum\":[\"NONE\",\"CORPORATE\",\"INDIVIDUAL\"]}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleEnumNoField() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestExtEnum.getDescriptor()),
            "io.anemos.metastore.core.proto.TestExtEnum");

    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestExtEnum\",\"type\":\"object\",\"properties\":{}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleNested() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleNested.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleNested");

    String valExpected =
        "{\"definitions\":{\"io.anemos.metastore.core.proto.TestSingleInt\":{\"$id\":\"#io.anemos.metastore.core.proto.TestSingleInt\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"integer\",\"minimum\":-2147483648,\"maximum\":2147483647}}}},\"title\":\"io.anemos.metastore.core.proto.TestSingleNested\",\"type\":\"object\",\"properties\":{\"testSingleInt\":{\"$ref\":\"#io.anemos.metastore.core.proto.TestSingleInt\"}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testMultipleRepeated() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestMultipleRepeated.getDescriptor()),
            "io.anemos.metastore.core.proto.TestMultipleRepeated");

    String valExpected =
        "{\"definitions\":{\"io.anemos.metastore.core.proto.TestSingleInt\":{\"$id\":\"#io.anemos.metastore.core.proto.TestSingleInt\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"integer\",\"minimum\":-2147483648,\"maximum\":2147483647}}}},\"title\":\"io.anemos.metastore.core.proto.TestMultipleRepeated\",\"type\":\"object\",\"properties\":{\"primitive_double\":{\"type\":\"array\",\"items\":{\"type\":\"numeric\"},\"minimum\":4.9E-324,\"maximum\":4.9E-324},\"primitive_int32\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"minimum\":-2147483648,\"maximum\":2147483647},\"primitive_bool\":{\"type\":\"array\",\"items\":{\"type\":\"boolean\"}},\"primitive_string\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"primitive_bytes\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"contentEncoding\":\"base64\"},\"enum_values\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"enum\":[\"ENUM1\",\"ENUM2\",\"ENUM3\"]}},\"message_int\":{\"type\":\"array\",\"items\":{\"$ref\":\"#io.anemos.metastore.core.proto.TestSingleInt\"}}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }
}
