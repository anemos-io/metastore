package io.anemos.metastore.core.proto;

import acme.jumpgate.v1alpha1.TestComplexJsonschema;
import org.junit.Assert;
import org.junit.Test;

public class ProtoToJsonSchemaTest {
  @Test
  public void testSingleLong() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleLong.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleLong");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleLong\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"integer\",\"minimum\":-9223372036854775808,\"maximum\":9223372036854775807}},\"required\":[\"field1\"]}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleInt() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleInt.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleInt");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleInt\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"integer\",\"minimum\":-2147483648,\"maximum\":2147483647}},\"required\":[\"field1\"]}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleUInt() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleUInt.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleUInt");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleUInt\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"integer\",\"minimum\":0,\"maximum\":4294967295}},\"required\":[\"field1\"]}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleBoolean() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleBoolean.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleBoolean");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleBoolean\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"boolean\"}},\"required\":[\"field1\"]}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleFloat() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleFloat.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleFloat");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleFloat\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"number\",\"minimum\":1.401298464324817E-45,\"maximum\":1.401298464324817E-45}},\"required\":[\"field1\"]}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleDouble() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleDouble.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleDouble");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleDouble\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"number\",\"minimum\":4.9E-324,\"maximum\":4.9E-324}},\"required\":[\"field1\"]}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleBytes() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleBytes.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleBytes");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleBytes\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"string\",\"contentEncoding\":\"base64\"}},\"required\":[\"field1\"]}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleString() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleString.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleString");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleString\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"string\"}},\"required\":[\"field1\"]}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleUInt64() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleUInt64.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleUInt64");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleUInt64\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"integer\",\"minimum\":0,\"maximum\":123456789012345678}},\"required\":[\"field1\"]}";
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
        "{\"definitions\":{\"io.anemos.metastore.core.proto.TestSingleInt\":{\"$id\":\"#io.anemos.metastore.core.proto.TestSingleInt\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"integer\",\"minimum\":-2147483648,\"maximum\":2147483647}},\"required\":[\"field1\"]}},\"title\":\"io.anemos.metastore.core.proto.TestSingleNested\",\"type\":\"object\",\"properties\":{\"testSingleInt\":{\"$ref\":\"#io.anemos.metastore.core.proto.TestSingleInt\"}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testMultipleRepeated() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestMultipleRepeated.getDescriptor()),
            "io.anemos.metastore.core.proto.TestMultipleRepeated");

    String valExpected =
        "{\"definitions\":{\"io.anemos.metastore.core.proto.TestSingleInt\":{\"$id\":\"#io.anemos.metastore.core.proto.TestSingleInt\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"integer\",\"minimum\":-2147483648,\"maximum\":2147483647}},\"required\":[\"field1\"]}},\"title\":\"io.anemos.metastore.core.proto.TestMultipleRepeated\",\"type\":\"object\",\"properties\":{\"primitive_double\":{\"type\":\"array\",\"items\":{\"type\":\"number\"},\"minimum\":4.9E-324,\"maximum\":4.9E-324},\"primitive_int32\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"minimum\":-2147483648,\"maximum\":2147483647},\"primitive_bool\":{\"type\":\"array\",\"items\":{\"type\":\"boolean\"}},\"primitive_string\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"primitive_bytes\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"contentEncoding\":\"base64\"},\"enum_values\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"enum\":[\"ENUM1\",\"ENUM2\",\"ENUM3\"]}},\"message_int\":{\"type\":\"array\",\"items\":{\"$ref\":\"#io.anemos.metastore.core.proto.TestSingleInt\"}}},\"required\":[\"primitive_double\",\"primitive_int32\",\"primitive_bool\",\"primitive_string\",\"primitive_bytes\"]}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testSingleTimestamp() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestSingleTimestamp.getDescriptor()),
            "io.anemos.metastore.core.proto.TestSingleTimestamp");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestSingleTimestamp\",\"type\":\"object\",\"properties\":{\"field1\":{\"type\":\"string\",\"pattern\":\"^[0-9]{4}[-](0[1-9]|1[012])[-](0[1-9]|[12][0-9]|3[01])[T]([01][1-9]|[2][0-3])[:]([0-5][0-9])[:]([0-5][0-9])([.][0-9]{0,9}){0,1}[Z]$\"}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testWrapperTypes() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestWrapperTypes.getDescriptor()),
            "io.anemos.metastore.core.proto.TestWrapperTypes");
    String valExpected =
        "{\"title\":\"io.anemos.metastore.core.proto.TestWrapperTypes\",\"type\":\"object\",\"properties\":{\"nullable_string\":{\"type\":\"string\"},\"nullable_int32\":{\"type\":\"integer\",\"minimum\":-2147483648,\"maximum\":2147483647},\"nullable_duration\":{\"type\":\"string\",\"pattern\":\"^[0-9]+([.][0-9]{0,9}){0,1}[s]$\"},\"nullable_boolean\":{\"type\":\"boolean\"},\"nullable_bytes\":{\"type\":\"string\",\"contentEncoding\":\"base64\"},\"nullable_double\":{\"type\":\"number\",\"minimum\":4.9E-324,\"maximum\":4.9E-324},\"nullable_float\":{\"type\":\"number\",\"minimum\":1.401298464324817E-45,\"maximum\":1.401298464324817E-45},\"nullable_int64\":{\"type\":\"integer\",\"minimum\":-9223372036854775808,\"maximum\":9223372036854775807},\"nullable_uint32\":{\"type\":\"integer\",\"minimum\":0,\"maximum\":4294967295},\"nullable_uint64\":{\"type\":\"integer\",\"minimum\":0,\"maximum\":123456789012345678}}}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }

  @Test
  public void testFullTypes() {
    final String jsonSingletype =
        ProtoToJsonSchema.convert(
            new PContainer(TestComplexJsonschema.Jump.getDescriptor()),
            "acme.jumpgate.v1alpha1.Jump");
    String valExpected =
        "{\"definitions\":{\"acme.jumpgate.v1alpha1.PowerUsed\":{\"$id\":\"#acme.jumpgate.v1alpha1.PowerUsed\",\"type\":\"object\",\"properties\":{\"terra_watt\":{\"type\":\"integer\",\"minimum\":-2147483648,\"maximum\":2147483647},\"ship_mass\":{\"type\":\"integer\",\"minimum\":-9223372036854775808,\"maximum\":9223372036854775807}},\"required\":[\"terra_watt\",\"ship_mass\"]},\"acme.jumpgate.v1alpha1.Gate\":{\"$id\":\"#acme.jumpgate.v1alpha1.Gate\",\"type\":\"object\",\"properties\":{\"uuid\":{\"type\":\"string\"}},\"required\":[\"uuid\"]}},\"title\":\"acme.jumpgate.v1alpha1.Jump\",\"type\":\"object\",\"properties\":{\"ulid\":{\"type\":\"string\"},\"heat\":{\"type\":\"integer\",\"minimum\":-2147483648,\"maximum\":2147483647},\"timestamp\":{\"type\":\"string\",\"pattern\":\"^[0-9]{4}[-](0[1-9]|1[012])[-](0[1-9]|[12][0-9]|3[01])[T]([01][1-9]|[2][0-3])[:]([0-5][0-9])[:]([0-5][0-9])([.][0-9]{0,9}){0,1}[Z]$\"},\"nullable_string\":{\"type\":\"string\"},\"hannes\":{\"type\":\"integer\",\"minimum\":-2147483648,\"maximum\":2147483647},\"power_used\":{\"$ref\":\"#acme.jumpgate.v1alpha1.PowerUsed\"},\"billing\":{\"type\":\"string\",\"enum\":[\"NONE\",\"CORPORATE\",\"INDIVIDUAL\",\"AUTHORITIES\"]},\"gate\":{\"$ref\":\"#acme.jumpgate.v1alpha1.Gate\"}},\"required\":[\"ulid\",\"heat\",\"hannes\"]}";
    Assert.assertEquals(valExpected, jsonSingletype);
  }
}
