package io.anemos.metastore.putils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TestSets {

  private static ProtoDomain load(String file) throws IOException {
    InputStream stream = new FileInputStream(new File("../testsets/" + file));
    return ProtoDomain.buildFrom(stream);
  }

  public static ProtoDomain base() throws IOException {
    return load("base.pb");
  }

  public static ProtoDomain source() throws IOException {
    return load("source.pb");
  }

  public static ProtoDomain baseDeprecateString() throws IOException {
    return load("base_deprecate_string.pb");
  }

  public static ProtoDomain baseRemoveString() throws IOException {
    return load("base_remove_string.pb");
  }

  public static ProtoDomain baseReserveString() throws IOException {
    return load("base_reserve_string.pb");
  }

  public static ProtoDomain baseReserveStringOnlyNumber() throws IOException {
    return load("base_reserve_string_only_number.pb");
  }

  public static ProtoDomain baseExtraFile() throws IOException {
    return load("base_add_file.pb");
  }

  public static ProtoDomain baseKnownOption() throws IOException {
    return load("base_known_option.pb");
  }

  public static ProtoDomain baseAddMessageOption() throws IOException {
    return load("base_add_message_option.pb");
  }

  public static ProtoDomain baseComplexMessageOptions() throws IOException {
    return load("base_complex_message_options.pb");
  }

  public static ProtoDomain baseChangeMessageOption() throws IOException {
    return load("base_change_message_option.pb");
  }

  public static ProtoDomain baseAddFieldOption() throws IOException {
    return load("base_add_field_option.pb");
  }

  public static ProtoDomain baseChangeFieldOption() throws IOException {
    return load("base_change_field_option.pb");
  }

  public static ProtoDomain baseAddFileOption() throws IOException {
    return load("base_add_file_option.pb");
  }

  public static ProtoDomain baseChangeFileOption() throws IOException {
    return load("base_change_file_option.pb");
  }

  public static ProtoDomain baseMultipleOptions() throws IOException {
    return load("base_multiple_options.pb");
  }
}
