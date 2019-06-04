package io.anemos.metastore;

import io.anemos.metastore.core.proto.PContainer;
import io.anemos.metastore.core.proto.profile.ProfileAvroEvolve;
import io.anemos.metastore.core.proto.profile.ValidationProfile;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ProtoLint;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.core.registry.AbstractRegistry;
import io.anemos.metastore.v1alpha1.Report;
import io.anemos.metastore.v1alpha1.ResultCount;
import io.anemos.metastore.v1alpha1.SchemaRegistyServiceGrpc;
import io.anemos.metastore.v1alpha1.Schemaregistry;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.IOException;

public class SchemaRegistryService extends SchemaRegistyServiceGrpc.SchemaRegistyServiceImplBase {

  private MetaStore metaStore;

  public SchemaRegistryService(MetaStore metaStore) {
    this.metaStore = metaStore;
  }

  @Override
  public void submitSchema(
      Schemaregistry.SubmitSchemaRequest request,
      StreamObserver<Schemaregistry.SubmitSchemaResponse> responseObserver) {
    schema(request, responseObserver, true);
  }

  @Override
  public void verifySchema(
      Schemaregistry.SubmitSchemaRequest request,
      StreamObserver<Schemaregistry.SubmitSchemaResponse> responseObserver) {
    schema(request, responseObserver, false);
  }

  public void schema(
      Schemaregistry.SubmitSchemaRequest request,
      StreamObserver<Schemaregistry.SubmitSchemaResponse> responseObserver,
      boolean submit) {
    PContainer in;
    try {
      in = new PContainer(request.getFdProtoSet().newInput());
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
      registry.update(report, in);
    }

    responseObserver.onNext(
        Schemaregistry.SubmitSchemaResponse.newBuilder().setReport(report).build());
    responseObserver.onCompleted();
  }

  private boolean hasErrors(Report report) {
    if (report.hasResultCount()) {
      ResultCount resultCount = report.getResultCount();
      return resultCount.getDiffErrors() > 0 || resultCount.getLintErrors() > 0;
    }
    return false;
  }

  private Report validate(
      Schemaregistry.SubmitSchemaRequest request, PContainer ref, PContainer in) {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(ref, in, results);
    ProtoLint lint = new ProtoLint(in, results);

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

    ValidationProfile profile = new ProfileAvroEvolve();
    return profile.validate(results.getReport());
  }

  @Override
  public void getSchema(
      Schemaregistry.GetSchemaRequest request,
      StreamObserver<Schemaregistry.GetSchemaResponse> responseObserver) {
    responseObserver.onNext(
        Schemaregistry.GetSchemaResponse.newBuilder()
            .setFdProtoSet(metaStore.registries.get(request.getRegistryName()).raw())
            .build());
    responseObserver.onCompleted();
  }
}
