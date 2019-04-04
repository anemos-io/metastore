package io.anemos.metastore.core.proto;

import java.io.IOException;
import java.io.InputStream;

public class TestSets {

    public static ProtoDescriptor base() throws IOException {
        InputStream resourceAsStream = TestSets.class.getResourceAsStream("base.pb");
        return new ProtoDescriptor(resourceAsStream);

    }

    public static ProtoDescriptor baseDeprecateString() throws IOException {
        InputStream resourceAsStream = TestSets.class.getResourceAsStream("base_deprecate_string.pb");
        return new ProtoDescriptor(resourceAsStream);

    }

    public static ProtoDescriptor baseRemoveString() throws IOException {
        InputStream resourceAsStream = TestSets.class.getResourceAsStream("base_remove_string.pb");
        return new ProtoDescriptor(resourceAsStream);
    }

    public static ProtoDescriptor baseReserveString() throws IOException {
        InputStream resourceAsStream = TestSets.class.getResourceAsStream("base_reserve_string.pb");
        return new ProtoDescriptor(resourceAsStream);
    }

    public static ProtoDescriptor baseReserveStringOnlyNumber() throws IOException {
        InputStream resourceAsStream = TestSets.class.getResourceAsStream("base_reserve_string_only_number.pb");
        return new ProtoDescriptor(resourceAsStream);
    }

    public static ProtoDescriptor baseExtraFile() throws IOException {
        InputStream resourceAsStream = TestSets.class.getResourceAsStream("base_add_file.pb");
        return new ProtoDescriptor(resourceAsStream);
    }
}
