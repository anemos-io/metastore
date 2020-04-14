package io.anemos.metastore;

import static io.anemos.metastore.v1alpha1.RegistryP.Merge.Strategy.REPLACE;

import com.google.protobuf.Descriptors;
import io.anemos.metastore.core.proto.profile.*;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ProtoLint;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.core.registry.AbstractRegistry;
import io.anemos.metastore.core.registry.ProtoPatch;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.BindP;
import io.anemos.metastore.v1alpha1.Patch;
import io.anemos.metastore.v1alpha1.RegistryGrpc;
import io.anemos.metastore.v1alpha1.RegistryP;
import io.anemos.metastore.v1alpha1.ValidationSummary;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistryService extends RegistryGrpc.RegistryImplBase {
  private static final Logger LOG = LoggerFactory.getLogger(RegistryService.class);

  private static final Measure.MeasureLong GET_FDS =
      Measure.MeasureLong.create(
          "get_schema_file_descriptors_count", "The number of file descriptors returned", "1");
  private static final StatsRecorder STATS_RECORDER = Stats.getStatsRecorder();

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

  private String validatePackage(String packageName) throws StatusException {
    if (packageName.contains("/")) {
      throw new StatusException(
          Status.fromCode(Status.Code.INVALID_ARGUMENT)
              .withDescription("Package name contains invalid /"));
    }
    if (packageName.endsWith(".")) {
      throw new StatusException(
          Status.fromCode(Status.Code.INVALID_ARGUMENT)
              .withDescription("Package name should not end with ."));
    }
    return packageName;
  }

  private String validateFileName(String fileName) throws StatusException {
    return fileName;
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
      ProtoDomain.Builder builder = registry.get().toBuilder();
      RegistryP.Merge mergeStrategy = request.getMergeStrategy();
      switch (mergeStrategy.getMergeStrategyCase()) {
        case PACKAGE_NAMES:
          if (mergeStrategy.getStrategy().equals(REPLACE)) {
            mergeStrategy.getPackageNames().getPackageNameList().forEach(builder::clearPackage);
          }
          for (String name1 : mergeStrategy.getPackageNames().getPackageNameList()) {
            builder.mergeInPackageBinary(name1, request.getFileDescriptorProtoList());
          }
          break;
        case PACKAGE_PREFIXES:
          if (mergeStrategy.getStrategy().equals(REPLACE)) {
            mergeStrategy
                .getPackagePrefixes()
                .getPackagePrefixList()
                .forEach(builder::clearPackagePrefix);
          }
          for (String s : mergeStrategy.getPackagePrefixes().getPackagePrefixList()) {
            builder.mergeInPackagePrefixBinary(s, request.getFileDescriptorProtoList());
          }
          break;
        case FILES:
          if (mergeStrategy.getStrategy().equals(REPLACE)) {
            mergeStrategy.getFiles().getFileNameList().forEach(builder::clearFile);
          }
          for (String name : mergeStrategy.getFiles().getFileNameList()) {
            builder.mergeFileBinary(name, request.getFileDescriptorProtoList());
          }
          break;
        case MERGESTRATEGY_NOT_SET:
        default:
          if (mergeStrategy.getStrategy().equals(REPLACE)) {
            builder.clearPackagePrefix("");
          }
          builder.mergeBinary(request.getFileDescriptorProtoList()).build();
          break;
      }
      in = builder.build();
    } catch (IOException | RuntimeException e) {
      LOG.error("Invalid FileDescriptor Set", e);
      responseObserver.onError(
          Status.fromCode(Status.Code.INVALID_ARGUMENT)
              .withDescription("Invalid FileDescriptor Set.")
              .withCause(e)
              .asRuntimeException());
      return;
    }

    Patch appliedPatch = createPatch(request.getMergeStrategy(), registry.get(), in);
    ValidationSummary summary = validate(request.getValidationProfile(), appliedPatch);
    if (submit) {
      if (hasErrors(summary)) {
        responseObserver.onError(
            Status.fromCode(Status.Code.FAILED_PRECONDITION)
                .withDescription("Incompatible schema, us verify to get errors.")
                .asRuntimeException());
        return;
      }
      registry.update(registry.ref(), in, appliedPatch, request.getNote());
    }

    responseObserver.onNext(
        RegistryP.SubmitSchemaResponse.newBuilder()
            .setAppliedPatch(appliedPatch)
            .setValidationSummary(summary)
            .build());
    responseObserver.onCompleted();
  }

  public void patch(
      RegistryP.PatchSchemaRequest request,
      StreamObserver<RegistryP.PatchSchemaResponse> responseObserver,
      boolean submit) {

    AbstractRegistry registry;
    try {
      registry = metaStore.registries.get(request.getRegistryName());
    } catch (StatusException e) {
      responseObserver.onError(e);
      return;
    }

    ProtoDomain in = ProtoPatch.apply(registry.get(), request.getPatch());

    Patch appliedPatch = createPatch(registry.get(), in);
    ValidationSummary summary = validate(request.getValidationProfile(), appliedPatch);
    if (submit) {
      if (hasErrors(summary)) {
        responseObserver.onError(
            Status.fromCode(Status.Code.FAILED_PRECONDITION)
                .withDescription("Incompatible schema, us verify to get errors.")
                .asRuntimeException());
        return;
      }
      registry.update(registry.ref(), in, appliedPatch, request.getNote());
    }

    responseObserver.onNext(
        RegistryP.PatchSchemaResponse.newBuilder()
            .setAppliedPatch(appliedPatch)
            .setValidationSummary(summary)
            .build());
    responseObserver.onCompleted();
  }

  private boolean hasErrors(ValidationSummary report) {
    return report.getDiffErrors() > 0 || report.getLintErrors() > 0;
  }

  private Patch createPatch(ProtoDomain ref, ProtoDomain in) {
    return createPatch(RegistryP.Merge.getDefaultInstance(), ref, in);
  }

  private Patch createPatch(RegistryP.Merge mergeStrategy, ProtoDomain ref, ProtoDomain in) {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(ref, in, results);
    ProtoLint lint = new ProtoLint(in, results);

    switch (mergeStrategy.getMergeStrategyCase()) {
      case MERGESTRATEGY_NOT_SET:
        diff.diffOnPackagePrefix("");
        lint.lintOnPackagePrefix("");
        break;
      case PACKAGE_PREFIXES:
        mergeStrategy
            .getPackagePrefixes()
            .getPackagePrefixList()
            .forEach(
                name -> {
                  diff.diffOnPackagePrefix(name);
                  lint.lintOnPackagePrefix(name);
                });
        break;
      case PACKAGE_NAMES:
        mergeStrategy
            .getPackageNames()
            .getPackageNameList()
            .forEach(
                name -> {
                  diff.diffOnPackage(name);
                  lint.lintOnPackage(name);
                });
        break;
      case FILES:
        mergeStrategy
            .getFiles()
            .getFileNameList()
            .forEach(
                name -> {
                  diff.diffOnFileName(name);
                  lint.lintOnFileName(name);
                });
        break;
      default:
        throw Status.fromCode(Status.Code.INTERNAL).asRuntimeException();
    }
    return results.createProto();
  }

  private ValidationSummary validate(String validationProfile, Patch patch) {
    ValidationProfile profile;
    switch (validationProfile) {
      case "allow:all":
        profile = new ProfileAllowAll();
        break;
      case "allow:none":
        profile = new ProfileAllowNone();
        break;
      case "proto:default":
        profile = new ProfileProtoEvolve();
        break;
      case "allow:stable:add:alpha:all":
        profile = new ProfileAllowStableAddAlphaAll();
        break;
      case "avro:forward":
      case "avro:backward":
      case "allow:add":
      case "":
        profile = new ProfileAllowAdd();
        break;
      default:
        throw Status.fromCode(Status.Code.INTERNAL).asRuntimeException();
    }

    return profile.validate(patch);
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
          BindP.ResourceBinding resourceBinding =
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
      STATS_RECORDER
          .newMeasureMap()
          .put(GET_FDS, schemaResponseBuilder.getFileDescriptorProtoCount())
          .record();
      responseObserver.onNext(schemaResponseBuilder.build());
      responseObserver.onCompleted();
    } catch (StatusException e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void verifyPatch(
      RegistryP.PatchSchemaRequest request,
      StreamObserver<RegistryP.PatchSchemaResponse> responseObserver) {
    patch(request, responseObserver, false);
  }

  @Override
  public void patchSchema(
      RegistryP.PatchSchemaRequest request,
      StreamObserver<RegistryP.PatchSchemaResponse> responseObserver) {
    patch(request, responseObserver, true);
  }
}
