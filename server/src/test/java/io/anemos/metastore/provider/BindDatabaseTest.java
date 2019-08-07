package io.anemos.metastore.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BindDatabaseTest {

  @Test
  public void experiments() throws Exception {

    BindDatabase io = new BindDatabase();

    io.bind("resouece", "test.foo");

    // OutputStreamWriter writer = new OutputStreamWriter(System.out);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    OutputStreamWriter writer = new OutputStreamWriter(byteArrayOutputStream);
    io.write(writer);
    writer.close();

    BindDatabase copy = new BindDatabase();
    copy.read(new InputStreamReader(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));
  }
}
