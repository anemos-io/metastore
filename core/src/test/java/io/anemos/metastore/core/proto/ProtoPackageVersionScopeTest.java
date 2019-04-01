package io.anemos.metastore.core.proto;

import com.google.protobuf.Descriptors;
import org.junit.Assert;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ProtoPackageVersionScopeTest {

    @Test
    public void testPackageVersionScope() throws ClassNotFoundException {
        Descriptors.Descriptor descriptorRef = TestPackageVersionScope.getDescriptor();
        Assert.assertEquals(Class.forName(descriptorRef.getFullName()).getPackage().getName()
                , "io.anemos.metastore.core.proto");
    }

    @Test
    public void testPackageVersionScopeFail() throws ClassNotFoundException{
        Descriptors.Descriptor descriptorRef = TestPackageVersionScope.getDescriptor();
        String packageExpected = Class.forName(descriptorRef.getFullName()).getPackage().getName();
        String packageActual = "io.anemos.metastore.core.test";
        try {
            Assert.assertEquals(packageExpected, packageActual);
        } catch (ComparisonFailure e) {
            Assert.fail("Error in comparison\n" +
                    "Expected: "  + packageExpected + "\n" +
                    "Actual: " + packageActual);
        }
    }
}
