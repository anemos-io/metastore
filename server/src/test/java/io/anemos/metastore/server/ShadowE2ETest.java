package io.anemos.metastore.server;

import io.anemos.metastore.MetaStore;
import io.anemos.metastore.RegistryService;
import io.anemos.metastore.config.GitRegistryConfig;
import io.anemos.metastore.config.MetaStoreConfig;
import io.anemos.metastore.config.ProviderConfig;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.core.proto.ProtocUtil;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.FieldChangeInfo;
import io.anemos.metastore.v1alpha1.RegistryGrpc;
import io.anemos.metastore.v1alpha1.RegistryP;
import io.anemos.metastore.v1alpha1.Report;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@Ignore
public class ShadowE2ETest {

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Rule public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule public TemporaryFolder localTempFolder = new TemporaryFolder();

  private static ProtoDomain baseKnownOption() throws IOException {
    InputStream resourceAsStream = ShadowE2ETest.class.getResourceAsStream("base_known_option.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  private static ProtoDomain baseKnownOptionAddField() throws IOException {
    InputStream resourceAsStream =
        ShadowE2ETest.class.getResourceAsStream("base_known_option_add_field.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  private static ProtoDomain baseAddMessageOption() throws IOException {
    InputStream resourceAsStream =
        ShadowE2ETest.class.getResourceAsStream("base_add_message_option.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  private static ProtoDomain shadowDefaultFieldAdded() throws IOException {
    InputStream resourceAsStream =
        ShadowE2ETest.class.getResourceAsStream("shadow_default_field_added.pb");
    return ProtoDomain.buildFrom(resourceAsStream);
  }

  @Before
  public void before() {
    environmentVariables.set("DEBUG", "true");
  }

  @Test
  public void shadowE2ELocalFileProvider() throws Exception {
    Path metastorePath = Files.createTempDirectory("metastore");
    Path shadowrepoPath = Files.createTempDirectory("shadowrepo");

    MetaStoreConfig config = new MetaStoreConfig();
    config.storage = new ProviderConfig();
    config.storage.providerClass = "io.anemos.metastore.provider.LocalFileStorage";
    config.storage.parameters =
        new ProviderConfig.Parameters[] {
          new ProviderConfig.Parameters("path", metastorePath.toAbsolutePath().toString())
        };
    config.registries =
        new RegistryConfig[] {
          new RegistryConfig("default"),
          new RegistryConfig("shadow", "default", new String[] {"test"})
        };
    config.registries[1].git = new GitRegistryConfig(shadowrepoPath.toAbsolutePath().toString());

    MetaStore metaStore = new MetaStore(config);

    RegistryGrpc.RegistryBlockingStub schemaRegistyStub = getSchemaRegistryStub(metaStore);

    RegistryP.SubmitSchemaRequest.Builder submitSchemaRequest =
        RegistryP.SubmitSchemaRequest.newBuilder()
            .setPackagePrefix("test")
            .setRegistryName("default");
    baseKnownOption()
        .iterator()
        .forEach(
            fileDescriptor ->
                submitSchemaRequest.addFileDescriptorProto(
                    fileDescriptor.toProto().toByteString()));
    schemaRegistyStub.submitSchema(submitSchemaRequest.build());

    // check default registry insides
    ProtoDomain actualDefaultRegistry =
        ProtoDomain.buildFrom(
            new File(metastorePath.toAbsolutePath().toString().concat("/default.pb")));
    Assert.assertEquals(
        baseKnownOption().toFileDescriptorSet(), actualDefaultRegistry.toFileDescriptorSet());

    // compile shadow repo files and compare
    ProtoDomain actualShadowRepo =
        ProtocUtil.createDescriptorSet(shadowrepoPath.toAbsolutePath().toString());
    Assert.assertEquals(
        baseKnownOption().toFileDescriptorSet(), actualShadowRepo.toFileDescriptorSet());

    // add option to shadow
    RegistryP.SubmitSchemaRequest.Builder submitSchemaRequest2 =
        RegistryP.SubmitSchemaRequest.newBuilder().setRegistryName("shadow");
    baseAddMessageOption()
        .iterator()
        .forEach(
            fileDescriptor ->
                submitSchemaRequest2.addFileDescriptorProto(
                    fileDescriptor.toProto().toByteString()));
    schemaRegistyStub.submitSchema(submitSchemaRequest2.build());

    // check shadow delta insides
    ValidationResults expectedResults = new ValidationResults();
    ProtoDiff protoDiff = new ProtoDiff(baseKnownOption(), baseAddMessageOption(), expectedResults);
    protoDiff.diffOnFileName("test/v1/simple.proto");
    Report actualShadowReport =
        Report.parseFrom(
            new FileInputStream(metastorePath.toAbsolutePath().toString() + "/shadow.pb"));
    Assert.assertEquals(
        expectedResults.getReport().getMessageResultsMap(),
        actualShadowReport.getMessageResultsMap());

    // add field to default
    RegistryP.SubmitSchemaRequest.Builder submitDefaultAddField =
        RegistryP.SubmitSchemaRequest.newBuilder().setPackagePrefix("test");
    baseKnownOptionAddField()
        .iterator()
        .forEach(
            fileDescriptor -> {
              submitDefaultAddField.addFileDescriptorProto(fileDescriptor.toProto().toByteString());
            });

    RegistryP.SubmitSchemaResponse verifyDefaultResponse2 =
        schemaRegistyStub.verifySchema(submitDefaultAddField.build());
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

    schemaRegistyStub.submitSchema(submitDefaultAddField.build());
    // check shadow insides
    actualShadowReport =
        Report.parseFrom(
            new FileInputStream(metastorePath.toAbsolutePath().toString() + "/shadow.pb"));
    Assert.assertEquals(
        expectedResults.getReport().getMessageResultsMap(),
        actualShadowReport.getMessageResultsMap());

    actualShadowRepo = ProtocUtil.createDescriptorSet(shadowrepoPath.toAbsolutePath().toString());
    Assert.assertEquals(
        shadowDefaultFieldAdded().toFileDescriptorSet(), actualShadowRepo.toFileDescriptorSet());
  }

  private RegistryGrpc.RegistryBlockingStub getSchemaRegistryStub(MetaStore metaStore)
      throws IOException {
    String serverName = InProcessServerBuilder.generateName();
    grpcCleanup.register(
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new RegistryService(metaStore))
            .build()
            .start());
    return RegistryGrpc.newBlockingStub(
        grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
  }
}
