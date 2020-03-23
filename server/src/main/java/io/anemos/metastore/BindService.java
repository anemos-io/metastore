package io.anemos.metastore;

import static io.anemos.metastore.v1alpha1.BindP.GetResourceBindingeRequest.SchemaContext;

import com.google.protobuf.Descriptors;
import io.anemos.metastore.core.registry.AbstractRegistry;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.BindGrpc;
import io.anemos.metastore.v1alpha1.BindP;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BindService extends BindGrpc.BindImplBase {
  private static final Logger LOG = LoggerFactory.getLogger(BindService.class);

  private static final Measure.MeasureLong GET_FDS =
      Measure.MeasureLong.create(
          "get_schema_file_descriptors_count", "The number of file descriptors returned", "1");
  private static final StatsRecorder STATS_RECORDER = Stats.getStatsRecorder();

  private MetaStore metaStore;

  public BindService(MetaStore metaStore) {

    this.metaStore = metaStore;
  }

  @Override
  public void createResourceBinding(
      BindP.CreateResourceBindingRequest request,
      StreamObserver<BindP.CreateResourceBindingResponse> responseObserver) {
    try {
      AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
      BindP.ResourceBinding resourceBinding = request.getBinding();
      registry.updateResourceBinding(resourceBinding, true);
      responseObserver.onNext(BindP.CreateResourceBindingResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusException e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void updateResourceBinding(
      BindP.UpdateResourceBindingRequest request,
      StreamObserver<BindP.UpdateResourceBindingResponse> responseObserver) {
    try {
      AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
      BindP.ResourceBinding resourceBinding = request.getBinding();
      registry.updateResourceBinding(resourceBinding, false);
      responseObserver.onNext(BindP.UpdateResourceBindingResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusException e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void getResourceBinding(
      BindP.GetResourceBindingeRequest request,
      StreamObserver<BindP.GetResourceBindingResponse> responseObserver) {
    try {
      AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
      BindP.ResourceBinding resourceBinding =
          registry.getResourceBinding(request.getLinkedResource());

      BindP.GetResourceBindingResponse.Builder response =
          BindP.GetResourceBindingResponse.newBuilder().setBinding(resourceBinding);

      ProtoDomain pContainer = registry.get();
      if (request.getSchemaContext() == SchemaContext.SCHEMA_CONTEXT_FULL_DOMAIN) {
        response.addAllFileDescriptorProto(
            pContainer.getFileDescriptors().stream()
                .map(fd -> fd.toProto().toByteString())
                .collect(Collectors.toList()));
      } else if (request.getSchemaContext() == SchemaContext.SCHEMA_CONTEXT_IN_SCOPE) {
        Collection<Descriptors.FileDescriptor> fds = new ArrayList<>();
        switch (resourceBinding.getTypeCase().getNumber()) {
          case BindP.ResourceBinding.MESSAGE_NAME_FIELD_NUMBER:
            Descriptors.Descriptor descriptor =
                pContainer.getDescriptorByName(resourceBinding.getMessageName());
            fds = pContainer.getDependantFileDescriptors(descriptor.getFile());
            break;
          case BindP.ResourceBinding.SERVICE_NAME_FIELD_NUMBER:
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
          case BindP.ResourceBinding.MESSAGE_NAME_FIELD_NUMBER:
            Descriptors.Descriptor descriptor =
                pContainer.getDescriptorByName(resourceBinding.getMessageName());
            response.addFileDescriptorProto(descriptor.getFile().toProto().toByteString());
            break;
          case BindP.ResourceBinding.SERVICE_NAME_FIELD_NUMBER:
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
      BindP.DeleteResourceBindingRequest request,
      StreamObserver<BindP.DeleteResourceBindingResponse> responseObserver) {
    try {
      AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
      registry.deleteResourceBinding(request.getLinkedResource());
      responseObserver.onNext(BindP.DeleteResourceBindingResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (StatusException e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void listResourceBindings(
      BindP.ListResourceBindingsRequest request,
      StreamObserver<BindP.ListResourceBindingsResponse> responseObserver) {
    try {
      AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
      BindP.ListResourceBindingsResponse.Builder builder =
          BindP.ListResourceBindingsResponse.newBuilder();
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
