package io.anemos.metastore;

import io.anemos.metastore.core.proto.PContainer;
import io.anemos.metastore.core.proto.profile.ProfileAvroEvolve;
import io.anemos.metastore.core.proto.profile.ValidationProfile;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ProtoLint;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.core.registry.AbstractRegistry;
import io.anemos.metastore.v1alpha1.Registry;
import io.anemos.metastore.v1alpha1.RegistyGrpc;
import io.anemos.metastore.v1alpha1.Report;
import io.anemos.metastore.v1alpha1.ResultCount;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.IOException;

public class RegistryService extends RegistyGrpc.RegistyImplBase {

  private MetaStore metaStore;

  public RegistryService(MetaStore metaStore) {
    this.metaStore = metaStore;
  }

  @Override
  public void submitSchema(
      Registry.SubmitSchemaRequest request,
      StreamObserver<Registry.SubmitSchemaResponse> responseObserver) {
    schema(request, responseObserver, true);
  }

  @Override
  public void verifySchema(
      Registry.SubmitSchemaRequest request,
      StreamObserver<Registry.SubmitSchemaResponse> responseObserver) {
    schema(request, responseObserver, false);
  }

  public void schema(
      Registry.SubmitSchemaRequest request,
      StreamObserver<Registry.SubmitSchemaResponse> responseObserver,
      boolean submit) {
    PContainer in;
    try {
      in = new PContainer(request.getFileDescriptorProtoList());
    } catch (IOException e) {
      responseObserver.onError(
          Status.fromCode(Status.Code.INVALID_ARGUMENT)
              .withDescription("Invalid FileDescriptor Set.")
              .withCause(e)
              .asRuntimeException());
      return;
    }

    AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
    Report report = validate(request, registry.get(), in);

    if (submit) {
      if (hasErrors(report)) {
        responseObserver.onError(
            Status.fromCode(Status.Code.FAILED_PRECONDITION)
                .withDescription("Incompatible schema, us verify to get errors.")
                .asRuntimeException());
        return;
      }
      registry.update(registry.ref(), in, report);
    }

    responseObserver.onNext(Registry.SubmitSchemaResponse.newBuilder().setReport(report).build());
    responseObserver.onCompleted();
  }

  private boolean hasErrors(Report report) {
    if (report.hasResultCount()) {
      ResultCount resultCount = report.getResultCount();
      return resultCount.getDiffErrors() > 0 || resultCount.getLintErrors() > 0;
    }
    return false;
  }

  private Report validate(Registry.SubmitSchemaRequest request, PContainer ref, PContainer in) {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(ref, in, results);
    ProtoLint lint = new ProtoLint(in, results);

    if (request.getScopeCount() == 0) {
      diff.diffOnPackagePrefix("");
      lint.lintOnPackagePrefix("");
    } else {
      request
          .getScopeList()
          .forEach(
              scope -> {
                switch (scope.getEntityScopeCase()) {
                  case FILE_NAME:
                    diff.diffOnFileName(scope.getFileName());
                    lint.lintOnFileName(scope.getFileName());
                    break;
                  case MESSAGE_NAME:
                    lint.lintOnMessage(scope.getMessageName());
                    break;
                  case SERVICE_NAME:
                    lint.lintOnService(scope.getServiceName());
                    break;
                  case ENUM_NAME:
                    lint.lintOnEnum(scope.getEnumName());
                    break;
                  default:
                    diff.diffOnPackagePrefix(scope.getPackagePrefix());
                    lint.lintOnPackagePrefix(scope.getPackagePrefix());
                }
              });
    }

    ValidationProfile profile = new ProfileAvroEvolve();
    return profile.validate(results.getReport());
  }

  @Override
  public void getSchema(
      Registry.GetSchemaRequest request,
      StreamObserver<Registry.GetSchemaResponse> responseObserver) {

    Registry.GetSchemaResponse.Builder schemaResponseBuilder =
        Registry.GetSchemaResponse.newBuilder();
    metaStore
        .registries
        .get(request.getRegistryName())
        .get()
        .iterator()
        .forEach(fd -> schemaResponseBuilder.addFileDescriptorProto(fd.toProto().toByteString()));

    // .setFdProtoSet(metaStore.registries.get(request.getRegistryName()).raw())

    responseObserver.onNext(schemaResponseBuilder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void createResourceBinding(
      Registry.CreateResourceBindingRequest request,
      StreamObserver<Registry.CreateResourceBindingResponse> responseObserver) {
    AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
    registry.createResourceBinding(request.getLinkedResource(), request.getMessageName());
    responseObserver.onNext(Registry.CreateResourceBindingResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void updateResourceBinding(
      Registry.UpdateResourceBindingRequest request,
      StreamObserver<Registry.UpdateResourceBindingResponse> responseObserver) {
    AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
    registry.updateResourceBinding(request.getLinkedResource(), request.getMessageName());
    responseObserver.onNext(Registry.UpdateResourceBindingResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void getResourceBinding(
      Registry.GetResourceBindingeRequest request,
      StreamObserver<Registry.GetResourceBindingResponse> responseObserver) {
    AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
    Registry.ResourceBinding resourceBinding =
        registry.getResourceBinding(request.getLinkedResource());
    responseObserver.onNext(
        Registry.GetResourceBindingResponse.newBuilder()
            .setMessageName(resourceBinding.getMessageName())
            .build());
    responseObserver.onCompleted();
  }

  @Override
  public void deleteResourceBinding(
      Registry.DeleteResourceBindingRequest request,
      StreamObserver<Registry.DeleteResourceBindingResponse> responseObserver) {
    AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
    registry.deleteResourceBinding(request.getLinkedResource());
    responseObserver.onNext(Registry.DeleteResourceBindingResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void listResourceBindings(
      Registry.ListResourceBindingsRequest request,
      StreamObserver<Registry.ListResourceBindingsResponse> responseObserver) {
    AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
    Registry.ListResourceBindingsResponse.Builder builder =
        Registry.ListResourceBindingsResponse.newBuilder();
    registry
        .listResourceBindings(request.getPageToken())
        .forEach(
            binding -> {
              builder.addBindings(binding);
            });
    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }
}
