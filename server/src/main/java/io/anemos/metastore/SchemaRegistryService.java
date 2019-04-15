package io.anemos.metastore;

import com.google.protobuf.ByteString;
import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.core.proto.profile.ProfileAvroEvolve;
import io.anemos.metastore.core.proto.profile.ValidationProfile;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ProtoLint;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.v1alpha1.Report;
import io.anemos.metastore.v1alpha1.SchemaRegistyServiceGrpc;
import io.anemos.metastore.v1alpha1.Schemaregistry;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

public class SchemaRegistryService extends SchemaRegistyServiceGrpc.SchemaRegistyServiceImplBase {

    MetaStore metaStore;

    public SchemaRegistryService(MetaStore metaStore) {
        this.metaStore = metaStore;
    }

    @Override
    public void submitSchema(Schemaregistry.SubmitSchemaRequest request, StreamObserver<Schemaregistry.SubmitSchemaResponse> responseObserver) {
        ProtoDescriptor in = null;
        try {
            in = new ProtoDescriptor(request.getFdProtoSet().newInput());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if ("shadowDelta".equals(request.getRegistryName())) {
            //Regenerate report
            Report report = validateShadow(request, in);
            try {
                metaStore.shadowRegistry.setDelta(report);
                metaStore.writeShadow();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            metaStore.repo = in;
            metaStore.writeDefault();
        }

        responseObserver.onNext(Schemaregistry.SubmitSchemaResponse
                .newBuilder()
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void verifySchema(Schemaregistry.SubmitSchemaRequest request, StreamObserver<Schemaregistry.SubmitSchemaResponse> responseObserver) {
        ProtoDescriptor in;
        try {
            in = new ProtoDescriptor(request.getFdProtoSet().newInput());
        } catch (IOException e) {
            responseObserver.onError(e);
            return;
        }

        Report report;
        if ("shadowDelta".equals(request.getRegistryName())) {
            report = validateShadow(request, in);
        } else {
            report = validateDefault(request, in);
        }

        responseObserver.onNext(Schemaregistry.SubmitSchemaResponse
                .newBuilder()
                .setReport(report)
                .build());
        responseObserver.onCompleted();
    }

    private Report validateDefault(Schemaregistry.SubmitSchemaRequest request, ProtoDescriptor in) {
        ValidationResults results = new ValidationResults();
        ProtoDiff diff = new ProtoDiff(metaStore.repo, in, results);
        ProtoLint lint = new ProtoLint(in, results);

        request.getScopeList().forEach(scope -> {
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

    private Report validateShadow(Schemaregistry.SubmitSchemaRequest request, ProtoDescriptor in) {
        //TODO validate on options

        ValidationResults results = new ValidationResults();
        //TODO metaStore.shadowDelta should contain cache (default + delta's)
        ProtoDiff diff = new ProtoDiff(metaStore.shadowRegistry.getShadow(), in, results);
        ProtoLint lint = new ProtoLint(in, results);

        request.getScopeList().forEach(scope -> {
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
    public void getSchema(Schemaregistry.GetSchemaRequest request, StreamObserver<Schemaregistry.GetSchemaResponse> responseObserver) {
        responseObserver.onNext(Schemaregistry.GetSchemaResponse
                .newBuilder()
                .setFdProtoSet(ByteString.copyFrom(metaStore.repo.toByteArray()))
                .build());
        responseObserver.onCompleted();
    }
}
