package io.anemos.metastore;

import static io.anemos.metastore.v1alpha1.RegistryP.GetResourceBindingeRequest.SchemaContext;

import com.google.protobuf.Descriptors;
import io.anemos.metastore.core.proto.profile.ProfileAllowAll;
import io.anemos.metastore.core.proto.profile.ProfileAllowNone;
import io.anemos.metastore.core.proto.profile.ProfileAvroEvolve;
import io.anemos.metastore.core.proto.profile.ProfileProtoEvolve;
import io.anemos.metastore.core.proto.profile.ValidationProfile;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ProtoLint;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.core.registry.AbstractRegistry;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.RegistryGrpc;
import io.anemos.metastore.v1alpha1.RegistryP;
import io.anemos.metastore.v1alpha1.Report;
import io.anemos.metastore.v1alpha1.ResultCount;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RegistryService extends RegistryGrpc.RegistryImplBase {

  private MetaStore metaStore;

  public RegistryService(MetaStore metaStore) {
    this.metaStore = metaStore;
  }

  @Override
  public void submitSchema(
      RegistryP.SubmitSchemaRequest request,
      StreamObserver<RegistryP.SubmitSchemaResponse> responseObserver) {
    schema(request, responseObserver, true);
  }

  @Override
  public void verifySchema(
      RegistryP.SubmitSchemaRequest request,
      StreamObserver<RegistryP.SubmitSchemaResponse> responseObserver) {
    schema(request, responseObserver, false);
  }

  public void schema(
      RegistryP.SubmitSchemaRequest request,
      StreamObserver<RegistryP.SubmitSchemaResponse> responseObserver,
      boolean submit) {

    AbstractRegistry registry;
    try {
      registry = metaStore.registries.get(request.getRegistryName());
    } catch (StatusException e) {
      responseObserver.onError(e);
      return;
    }

    ProtoDomain in;
    try {
      switch (request.getEntityScopeCase()) {
        case PACKAGE_NAME:
          in = ProtoDomain.empty();
          break;
        case PACKAGE_PREFIX:
          in = ProtoDomain.empty();
          break;
        case FILE_NAME:
          in = ProtoDomain.empty();
          break;
        case ENTITYSCOPE_NOT_SET:
        default:
          in = registry.get().toBuilder().mergeBinary(request.getFileDescriptorProtoList()).build();
      }
    } catch (IOException e) {
      responseObserver.onError(
          Status.fromCode(Status.Code.INVALID_ARGUMENT)
              .withDescription("Invalid FileDescriptor Set.")
              .withCause(e)
              .asRuntimeException());
      return;
    }

    try {
      Report report = validate(registry, request, registry.get(), in);

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

      responseObserver.onNext(
          RegistryP.SubmitSchemaResponse.newBuilder().setReport(report).build());
      responseObserver.onCompleted();
    } catch (StatusException e) {
      responseObserver.onError(e);
    }
  }

  private boolean hasErrors(Report report) {
    if (report.hasResultCount()) {
      ResultCount resultCount = report.getResultCount();
      return resultCount.getDiffErrors() > 0 || resultCount.getLintErrors() > 0;
    }
    return false;
  }

  private Report validate(
      AbstractRegistry registry,
      RegistryP.SubmitSchemaRequest request,
      ProtoDomain ref,
      ProtoDomain in)
      throws StatusException {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(ref, in, results);
    ProtoLint lint = new ProtoLint(in, results);

    switch (request.getEntityScopeCase()) {
      case ENTITYSCOPE_NOT_SET:
        diff.diffOnPackagePrefix("");
        lint.lintOnPackagePrefix("");
        break;
      case PACKAGE_PREFIX:
        diff.diffOnPackagePrefix(request.getPackagePrefix());
        lint.lintOnPackagePrefix(request.getPackagePrefix());
        break;
      case PACKAGE_NAME:
        break;
      case FILE_NAME:
        diff.diffOnFileName(request.getFileName());
        lint.lintOnFileName(request.getFileName());
        break;
      default:
        throw Status.fromCode(Status.Code.INTERNAL).asRuntimeException();
    }

    ValidationProfile profile;
    switch (request.getValidationProfile()) {
      case "allow:all":
        profile = new ProfileAllowAll();
        break;
      case "allow:none":
        profile = new ProfileAllowNone();
        break;
      case "proto:default":
        profile = new ProfileProtoEvolve();
        break;
      case "avro:forward":
      case "avro:backward":
      default:
        profile = new ProfileAvroEvolve();
    }

    return profile.validate(results.getReport());
  }

  @Override
  public void getSchema(
      RegistryP.GetSchemaRequest request,
      StreamObserver<RegistryP.GetSchemaResponse> responseObserver) {
    try {
      RegistryP.GetSchemaResponse.Builder schemaResponseBuilder =
          RegistryP.GetSchemaResponse.newBuilder();

      AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
      ProtoDomain pContainer = registry.get();

      List<Descriptors.FileDescriptor> fdl = new ArrayList<>();
      switch (request.getEntityScopeCase()) {
        case PACKAGE_PREFIX:
          fdl = pContainer.getFileDescriptorsByPackagePrefix(request.getPackagePrefix());
          break;
        case PACKAGE_NAME:
          fdl = pContainer.getFileDescriptorsByPackageName(request.getPackageName());
          break;
        case MESSAGE_NAME:
          Descriptors.Descriptor descriptor =
              pContainer.getDescriptorByName(request.getMessageName());
          if (descriptor != null) {
            fdl.add(descriptor.getFile());
          }
          break;
        case SERVICE_NAME:
          Descriptors.ServiceDescriptor serviceDescriptor =
              pContainer.getServiceDescriptorByName(request.getServiceName());
          if (serviceDescriptor != null) {
            fdl.add(serviceDescriptor.getFile());
          }
          break;
        case ENUM_NAME:
          Descriptors.EnumDescriptor enumDescriptor =
              pContainer.getEnumDescriptorByName(request.getServiceName());
          if (enumDescriptor != null) {
            fdl.add(enumDescriptor.getFile());
          }
          break;
        case FILE_NAME:
          Descriptors.FileDescriptor fileDescriptor =
              pContainer.getFileDescriptorByFileName(request.getFileName());
          if (fileDescriptor != null) {
            fdl.add(fileDescriptor);
          }
          break;
        case LINKED_RESOURCE:
          RegistryP.ResourceBinding resourceBinding =
              registry.getResourceBinding(request.getLinkedResource());
          switch (resourceBinding.getTypeCase()) {
            case MESSAGE_NAME:
              Descriptors.Descriptor linkedDescriptor =
                  pContainer.getDescriptorByName(resourceBinding.getMessageName());
              if (linkedDescriptor == null) {
                throw Status.fromCode(Status.Code.NOT_FOUND)
                    .withDescription("Message referenced in binding not found.")
                    .asException();
              }
              fdl.add(linkedDescriptor.getFile());
              break;
            case SERVICE_NAME:
              Descriptors.ServiceDescriptor linkedServiceDescriptor =
                  pContainer.getServiceDescriptorByName(resourceBinding.getServiceName());
              if (linkedServiceDescriptor == null) {
                throw Status.fromCode(Status.Code.NOT_FOUND)
                    .withDescription("Service referenced in binding not found.")
                    .asException();
              }
              fdl.add(linkedServiceDescriptor.getFile());
              break;
            case TYPE_NOT_SET:
            default:
              throw Status.fromCode(Status.Code.INTERNAL).asRuntimeException();
          }
          break;
        case ENTITYSCOPE_NOT_SET:
          fdl = pContainer.getFileDescriptors();
          break;
        default:
          throw Status.fromCode(Status.Code.INTERNAL).asRuntimeException();
      }
      if (fdl.size() == 0) {
        throw Status.fromCode(Status.Code.NOT_FOUND)
            .withDescription("No descriptors matching the search criteria.")
            .asException();
      }
      if (request.getTransitive()) {
        schemaResponseBuilder.addAllFileDescriptorProto(
            pContainer.getDependantFileDescriptors(fdl).stream()
                .map(fd -> fd.toProto().toByteString())
                .collect(Collectors.toList()));
      } else {
        schemaResponseBuilder.addAllFileDescriptorProto(
            fdl.stream().map(fd -> fd.toProto().toByteString()).collect(Collectors.toList()));
      }

      responseObserver.onNext(schemaResponseBuilder.build());
      responseObserver.onCompleted();
    } catch (StatusException e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void createResourceBinding(
      RegistryP.CreateResourceBindingRequest request,
      StreamObserver<RegistryP.CreateResourceBindingResponse> responseObserver) {
    try {
      AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
      RegistryP.ResourceBinding resourceBinding = request.getBinding();
      registry.updateResourceBinding(resourceBinding, true);
      responseObserver.onNext(RegistryP.CreateResourceBindingResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusException e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void updateResourceBinding(
      RegistryP.UpdateResourceBindingRequest request,
      StreamObserver<RegistryP.UpdateResourceBindingResponse> responseObserver) {
    try {
      AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
      RegistryP.ResourceBinding resourceBinding = request.getBinding();
      registry.updateResourceBinding(resourceBinding, false);
      responseObserver.onNext(RegistryP.UpdateResourceBindingResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusException e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void getResourceBinding(
      RegistryP.GetResourceBindingeRequest request,
      StreamObserver<RegistryP.GetResourceBindingResponse> responseObserver) {
    try {
      AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
      RegistryP.ResourceBinding resourceBinding =
          registry.getResourceBinding(request.getLinkedResource());

      RegistryP.GetResourceBindingResponse.Builder response =
          RegistryP.GetResourceBindingResponse.newBuilder().setBinding(resourceBinding);

      ProtoDomain pContainer = registry.get();
      if (request.getSchemaContext() == SchemaContext.SCHEMA_CONTEXT_FULL_DOMAIN) {
        response.addAllFileDescriptorProto(
            pContainer.getFileDescriptors().stream()
                .map(fd -> fd.toProto().toByteString())
                .collect(Collectors.toList()));
      } else if (request.getSchemaContext() == SchemaContext.SCHEMA_CONTEXT_IN_SCOPE) {
        Collection<Descriptors.FileDescriptor> fds = new ArrayList<>();
        switch (resourceBinding.getTypeCase().getNumber()) {
          case RegistryP.ResourceBinding.MESSAGE_NAME_FIELD_NUMBER:
            Descriptors.Descriptor descriptor =
                pContainer.getDescriptorByName(resourceBinding.getMessageName());
            fds = pContainer.getDependantFileDescriptors(descriptor.getFile());
            break;
          case RegistryP.ResourceBinding.SERVICE_NAME_FIELD_NUMBER:
            Descriptors.ServiceDescriptor service =
                pContainer.getServiceDescriptorByName(resourceBinding.getServiceName());
            fds = pContainer.getDependantFileDescriptors(service.getFile());
            break;
          default:
            throw Status.fromCode(Status.Code.INTERNAL)
                .withDescription("Linked resource isn't linked to a descriptor")
                .asRuntimeException();
        }
        response.addAllFileDescriptorProto(
            fds.stream().map(fd -> fd.toProto().toByteString()).collect(Collectors.toList()));
      } else if (request.getSchemaContext() == SchemaContext.SCHEMA_CONTEXT_IN_FILE) {
        switch (resourceBinding.getTypeCase().getNumber()) {
          case RegistryP.ResourceBinding.MESSAGE_NAME_FIELD_NUMBER:
            Descriptors.Descriptor descriptor =
                pContainer.getDescriptorByName(resourceBinding.getMessageName());
            response.addFileDescriptorProto(descriptor.getFile().toProto().toByteString());
            break;
          case RegistryP.ResourceBinding.SERVICE_NAME_FIELD_NUMBER:
            Descriptors.ServiceDescriptor service =
                pContainer.getServiceDescriptorByName(resourceBinding.getServiceName());
            response.addFileDescriptorProto(service.getFile().toProto().toByteString());
            break;
          default:
            throw Status.fromCode(Status.Code.INTERNAL)
                .withDescription("Linked resource isn't linked to a descriptor")
                .asRuntimeException();
        }
      }
      responseObserver.onNext(response.build());
      responseObserver.onCompleted();
    } catch (StatusException e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void deleteResourceBinding(
      RegistryP.DeleteResourceBindingRequest request,
      StreamObserver<RegistryP.DeleteResourceBindingResponse> responseObserver) {
    try {
      AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
      registry.deleteResourceBinding(request.getLinkedResource());
      responseObserver.onNext(RegistryP.DeleteResourceBindingResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusException e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void listResourceBindings(
      RegistryP.ListResourceBindingsRequest request,
      StreamObserver<RegistryP.ListResourceBindingsResponse> responseObserver) {
    try {
      AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
      RegistryP.ListResourceBindingsResponse.Builder builder =
          RegistryP.ListResourceBindingsResponse.newBuilder();
      registry
          .listResourceBindings(request.getPageToken())
          .forEach(
              binding -> {
                builder.addBindings(binding);
              });
      responseObserver.onNext(builder.build());
      responseObserver.onCompleted();
    } catch (StatusException e) {
      responseObserver.onError(e);
    }
  }
}
