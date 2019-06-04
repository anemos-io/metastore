package io.anemos.metastore.core.proto;

import java.io.IOException;
import java.io.InputStream;

public class TestSets {

  public static PContainer base() throws IOException {
    InputStream resourceAsStream = TestSets.class.getResourceAsStream("../base.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseDeprecateString() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_deprecate_string.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseRemoveString() throws IOException {
    InputStream resourceAsStream = TestSets.class.getResourceAsStream("../base_remove_string.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseReserveString() throws IOException {
    InputStream resourceAsStream = TestSets.class.getResourceAsStream("../base_reserve_string.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseReserveStringOnlyNumber() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_reserve_string_only_number.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseExtraFile() throws IOException {
    InputStream resourceAsStream = TestSets.class.getResourceAsStream("../base_add_file.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseKnownOption() throws IOException {
    InputStream resourceAsStream = TestSets.class.getResourceAsStream("../base_known_option.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseAddMessageOption() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_add_message_option.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseComplexMessageOptions() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_complex_message_options.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseChangeMessageOption() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_change_message_option.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseAddFieldOption() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_add_field_option.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseChangeFieldOption() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_change_field_option.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseAddFileOption() throws IOException {
    InputStream resourceAsStream = TestSets.class.getResourceAsStream("../base_add_file_option.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseChangeFileOption() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_change_file_option.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseMultipleOptions() throws IOException {
    InputStream resourceAsStream =
        TestSets.class.getResourceAsStream("../base_multiple_options.pb");
    return new PContainer(resourceAsStream);
  }
}
