package io.anemos.metastore.server;

import io.anemos.metastore.MetaStore;
import io.anemos.metastore.SchemaRegistryService;
import io.anemos.metastore.config.GitConfig;
import io.anemos.metastore.config.MetaStoreConfig;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.config.StorageProviderConfig;
import io.anemos.metastore.core.proto.PContainer;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

  private static PContainer baseKnownOption() throws IOException {
    InputStream resourceAsStream = ShadowE2ETest.class.getResourceAsStream("base_known_option.pb");
    return new PContainer(resourceAsStream);
  }

  private static PContainer baseKnownOptionAddField() throws IOException {
    InputStream resourceAsStream =
        ShadowE2ETest.class.getResourceAsStream("base_known_option_add_field.pb");
    return new PContainer(resourceAsStream);
  }

  private static PContainer baseAddMessageOption() throws IOException {
    InputStream resourceAsStream =
        ShadowE2ETest.class.getResourceAsStream("base_add_message_option.pb");
    return new PContainer(resourceAsStream);
  }

  private static PContainer shadowDefaultFieldAdded() throws IOException {
    InputStream resourceAsStream =
        ShadowE2ETest.class.getResourceAsStream("shadow_default_field_added.pb");
    return new PContainer(resourceAsStream);
  }

  @Before
  public void before() throws Exception {
    environmentVariables.set("DEBUG", "true");
  }

  @Test
  public void shadowE2ELocalFileProvider() throws Exception {
    Path metastorePath = Files.createTempDirectory("metastore");
    Path shadowrepoPath = Files.createTempDirectory("shadowrepo");

    MetaStoreConfig config = new MetaStoreConfig();
    config.storage = new StorageProviderConfig();
    config.storage.providerClass = "io.anemos.metastore.provider.LocalFileProvider";
    config.storage.parameters =
        new StorageProviderConfig.Parameters[] {
          new StorageProviderConfig.Parameters("path", metastorePath.toAbsolutePath().toString())
        };
    config.registries =
        new RegistryConfig[] {
          new RegistryConfig("default"), new RegistryConfig("shadow", "default")
        };
    config.registries[1].git = new GitConfig(shadowrepoPath.toAbsolutePath().toString());

    MetaStore metaStore = new MetaStore(config);

    SchemaRegistyServiceGrpc.SchemaRegistyServiceBlockingStub schemaRegistyStub =
        getSchemaRegistryStub(metaStore);

    schemaRegistyStub.submitSchema(
        Schemaregistry.SubmitSchemaRequest.newBuilder()
            .setFdProtoSet(baseKnownOption().toByteString())
            .addScope(Schemaregistry.Scope.newBuilder().setPackagePrefix("test").build())
            .setRegistryName("default")
            .build());

    // check default registry insides
    PContainer actualDefaultRegistry =
        new PContainer(metastorePath.toAbsolutePath().toString().concat("/default.pb"));
    Assert.assertEquals(
        baseKnownOption().toFileDescriptorSet(), actualDefaultRegistry.toFileDescriptorSet());

    // compile shadow repo files and compare
    PContainer actualShadowRepo =
        ProtocUtil.createDescriptorSet(shadowrepoPath.toAbsolutePath().toString());
    Assert.assertEquals(
        baseKnownOption().toFileDescriptorSet(), actualShadowRepo.toFileDescriptorSet());

    // add option to shadow
    schemaRegistyStub.submitSchema(
        Schemaregistry.SubmitSchemaRequest.newBuilder()
            .setFdProtoSet(baseAddMessageOption().toByteString())
            .addScope(Schemaregistry.Scope.newBuilder().setPackagePrefix("test").build())
            .setRegistryName("shadow")
            .build());

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
    Schemaregistry.SubmitSchemaRequest submitDefaultAddField =
        Schemaregistry.SubmitSchemaRequest.newBuilder()
            .setFdProtoSet(baseKnownOptionAddField().toByteString())
            .addScope(Schemaregistry.Scope.newBuilder().setPackagePrefix("test").build())
            .build();
    Schemaregistry.SubmitSchemaResponse verifyDefaultResponse2 =
        schemaRegistyStub.verifySchema(submitDefaultAddField);
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

    schemaRegistyStub.submitSchema(submitDefaultAddField);
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

  private SchemaRegistyServiceGrpc.SchemaRegistyServiceBlockingStub getSchemaRegistryStub(
      MetaStore metaStore) throws IOException {
    String serverName = InProcessServerBuilder.generateName();
    grpcCleanup.register(
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new SchemaRegistryService(metaStore))
            .build()
            .start());
    return SchemaRegistyServiceGrpc.newBlockingStub(
        grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
  }
}
