package io.anemos.metastore.server;

import io.anemos.metastore.MetaStore;
import io.anemos.metastore.SchemaRegistryService;
import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.core.proto.ProtocUtil;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.v1alpha1.FieldChangeInfo;
import io.anemos.metastore.v1alpha1.Report;
import io.anemos.metastore.v1alpha1.SchemaRegistyServiceGrpc;
import io.anemos.metastore.v1alpha1.Schemaregistry;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ShadowE2ETest {

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Rule public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule public TemporaryFolder localTempFolder = new TemporaryFolder();

  @Before
  public void before() throws Exception {
    environmentVariables.set("DEBUG", "true");
  }

  @Test
  public void shadowE2ELocalFileProvider() throws Exception {
    File metaStoreFolder = localTempFolder.newFolder("metastore");
    File shadowRepoFolder = localTempFolder.newFolder("shadowrepo");
    environmentVariables.set("METASTORE_PATH", metaStoreFolder.getPath());
    environmentVariables.set("METASTORE_SHADOW_REPO_PATH", shadowRepoFolder.getPath());
    environmentVariables.set(
        "METASTORE_STORAGE_PROVIDER", "io.anemos.metastore.provider.LocalFileProvider");
    MetaStore metaStore = new MetaStore();

    String serverName = InProcessServerBuilder.generateName();
    grpcCleanup.register(
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new SchemaRegistryService(metaStore))
            .build()
            .start());
    SchemaRegistyServiceGrpc.SchemaRegistyServiceBlockingStub blockingStub =
        SchemaRegistyServiceGrpc.newBlockingStub(
            grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build()));

    Schemaregistry.SubmitSchemaRequest submitDefault =
        Schemaregistry.SubmitSchemaRequest.newBuilder()
            .setFdProtoSet(baseKnownOption().toByteString())
            .addScope(Schemaregistry.Scope.newBuilder().setPackagePrefix("test").build())
            .setRegistryName("default")
            .build();
    Schemaregistry.SubmitSchemaResponse verifyDefaultResponse =
        blockingStub.verifySchema(submitDefault);
    Assert.assertFalse(verifyDefaultResponse.getReport().getResultCount().getDiffErrors() > 0);
    blockingStub.submitSchema(submitDefault);

    // check default registry insides
    ProtoDescriptor actualDefaultRegistry =
        new ProtoDescriptor(metaStoreFolder.getCanonicalPath().concat("/default.pb"));
    Assert.assertEquals(
        baseKnownOption().toFileDescriptorSet(), actualDefaultRegistry.toFileDescriptorSet());

    // compile shadow repo files and compare
    ProtoDescriptor actualShadowRepo =
        ProtocUtil.createDescriptorSet(shadowRepoFolder.getCanonicalPath());
    Assert.assertEquals(
        baseKnownOption().toFileDescriptorSet(), actualShadowRepo.toFileDescriptorSet());

    // add option to shadow
    Schemaregistry.SubmitSchemaRequest submitShadow =
        Schemaregistry.SubmitSchemaRequest.newBuilder()
            .setFdProtoSet(baseAddMessageOption().toByteString())
            .addScope(Schemaregistry.Scope.newBuilder().setPackagePrefix("test").build())
            .setRegistryName("shadow")
            .build();
    Schemaregistry.SubmitSchemaResponse verifyShadowResponse =
        blockingStub.verifySchema(submitShadow);
    Assert.assertFalse(verifyShadowResponse.getReport().getResultCount().getDiffErrors() > 0);

    blockingStub.submitSchema(submitShadow);
    // check shadow delta insides
    ValidationResults expectedResults = new ValidationResults();
    ProtoDiff protoDiff = new ProtoDiff(baseKnownOption(), baseAddMessageOption(), expectedResults);
    protoDiff.diffOnFileName("test/v1/simple.proto");
    Report actualShadowReport =
        Report.parseFrom(new FileInputStream(metaStoreFolder.getPath() + "/shadow.pb"));
    Assert.assertEquals(
        expectedResults.getReport().getMessageResultsMap(),
        actualShadowReport.getMessageResultsMap());

    // add field to default
    Schemaregistry.SubmitSchemaRequest submitDefaultAddField =
        Schemaregistry.SubmitSchemaRequest.newBuilder()
            .setFdProtoSet(baseKnownOptionAddField().toByteString())
            .addScope(Schemaregistry.Scope.newBuilder().setPackagePrefix("test").build())
            .build();
    Schemaregistry.SubmitSchemaResponse verifyDefaultResponse2 =
        blockingStub.verifySchema(submitDefaultAddField);
    Assert.assertFalse(verifyDefaultResponse2.getReport().getResultCount().getDiffErrors() > 0);
    Assert.assertEquals(
        FieldChangeInfo.FieldChangeType.FIELD_ADDED,
        verifyDefaultResponse2
            .getReport()
            .getMessageResultsMap()
            .get("test.v1.ProtoBeamBasicMessage")
            .getFieldResults(0)
            .getChange()
            .getChangeType());

    blockingStub.submitSchema(submitDefaultAddField);
    // check shadow insides
    actualShadowReport =
        Report.parseFrom(new FileInputStream(metaStoreFolder.getPath() + "/shadow.pb"));
    Assert.assertEquals(
        expectedResults.getReport().getMessageResultsMap(),
        actualShadowReport.getMessageResultsMap());

    actualShadowRepo = ProtocUtil.createDescriptorSet(shadowRepoFolder.getCanonicalPath());
    Assert.assertEquals(
        shadowDefaultFieldAdded().toFileDescriptorSet(), actualShadowRepo.toFileDescriptorSet());
  }

  private static ProtoDescriptor baseKnownOption() throws IOException {
    InputStream resourceAsStream = ShadowE2ETest.class.getResourceAsStream("base_known_option.pb");
    return new ProtoDescriptor(resourceAsStream);
  }

  private static ProtoDescriptor baseKnownOptionAddField() throws IOException {
    InputStream resourceAsStream =
        ShadowE2ETest.class.getResourceAsStream("base_known_option_add_field.pb");
    return new ProtoDescriptor(resourceAsStream);
  }

  private static ProtoDescriptor baseAddMessageOption() throws IOException {
    InputStream resourceAsStream =
        ShadowE2ETest.class.getResourceAsStream("base_add_message_option.pb");
    return new ProtoDescriptor(resourceAsStream);
  }

  private static ProtoDescriptor shadowDefaultFieldAdded() throws IOException {
    InputStream resourceAsStream =
        ShadowE2ETest.class.getResourceAsStream("shadow_default_field_added.pb");
    return new ProtoDescriptor(resourceAsStream);
  }
}
