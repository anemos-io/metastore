package io.anemos.metastore.core.proto;

import io.anemos.metastore.putils.ProtoDomain;
import java.io.IOException;
import java.io.InputStream;

public class TestSets {

  public static ProtoDomain base() throws IOException {
    InputStream resourceAsStream = TestSets.class.getResourceAsStream("../base.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain source() throws IOException {
    InputStream resourceAsStream = TestSets.class.getResourceAsStream("../source.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain baseDeprecateString() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_deprecate_string.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain baseRemoveString() throws IOException {
    InputStream resourceAsStream = TestSets.class.getResourceAsStream("../base_remove_string.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain baseReserveString() throws IOException {
    InputStream resourceAsStream = TestSets.class.getResourceAsStream("../base_reserve_string.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain baseReserveStringOnlyNumber() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_reserve_string_only_number.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain baseExtraFile() throws IOException {
    InputStream resourceAsStream = TestSets.class.getResourceAsStream("../base_add_file.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain baseKnownOption() throws IOException {
    InputStream resourceAsStream = TestSets.class.getResourceAsStream("../base_known_option.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain baseAddMessageOption() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_add_message_option.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain baseComplexMessageOptions() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_complex_message_options.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain baseChangeMessageOption() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_change_message_option.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain baseAddFieldOption() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_add_field_option.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain baseChangeFieldOption() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_change_field_option.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain baseAddFileOption() throws IOException {
    InputStream resourceAsStream = TestSets.class.getResourceAsStream("../base_add_file_option.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain baseChangeFileOption() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_change_file_option.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  public static ProtoDomain baseMultipleOptions() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_multiple_options.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }
}
