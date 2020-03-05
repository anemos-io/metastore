package io.anemos.metastore.putils;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProtoDomainTreeShaker {

    private ProtoDomain sourceDomain;
    private Set<Descriptors.Descriptor> roots = new HashSet<>();
    private boolean removeSource = false;

    public static ProtoDomainTreeShaker from(ProtoDomain domain) {
        ProtoDomainTreeShaker shaker = new ProtoDomainTreeShaker();
        shaker.sourceDomain = domain;
        return shaker;
    }

    public ProtoDomainTreeShaker withDescriptor(Descriptors.Descriptor descriptor) {
        roots.add(descriptor);
        return this;
    }

    public ProtoDomainTreeShaker withDescriptor(String messageName) {
        return withDescriptor(sourceDomain.getDescriptor(messageName));
    }

    public ProtoDomainTreeShaker removeSource(boolean removeSource) {
        this.removeSource = removeSource;
        return this;
    }

    private void collectDependencies(Set<Descriptors.FileDescriptor> set, Descriptors.FileDescriptor fd) {
        if(set.contains(fd)) {
            return;
        }
        set.add(fd);
        fd.getDependencies().forEach(dependency -> collectDependencies(set, dependency));
    }

    public ProtoDomain shake() throws InvalidProtocolBufferException {
        Set<Descriptors.FileDescriptor> fileDescriptors = new HashSet<>();
        for(Descriptors.Descriptor descriptor : roots) {
            collectDependencies(fileDescriptors, descriptor.getFile());
        }

        List<ByteString> protos = new ArrayList<>();
        fileDescriptors.forEach(fd -> {
            DescriptorProtos.FileDescriptorProto proto = fd.toProto();
            if(removeSource) {
                protos.add(proto.toBuilder().clearSourceCodeInfo().build().toByteString());
            }
            else {
                protos.add(proto.toByteString());
            }
        });
        return ProtoDomain.buildFrom(protos);
    }
}
