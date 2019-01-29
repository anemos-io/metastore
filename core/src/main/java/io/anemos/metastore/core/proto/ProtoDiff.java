package io.anemos.metastore.core.proto;

import com.google.protobuf.DescriptorProtos;

import java.util.*;

public class ProtoDiff {

    private DescriptorProtos.FileDescriptorProto fd_ref;
    private DescriptorProtos.FileDescriptorProto fd_new;

    ProtoDiff(DescriptorProtos.FileDescriptorProto fd_ref, DescriptorProtos.FileDescriptorProto fd_new) {
        this.fd_ref = fd_ref;
        this.fd_new = fd_new;
    }

    public void diff() {

        diffMessageTypes(fd_ref.getMessageTypeList(), fd_new.getMessageTypeList());
        diffEnumTypes(fd_ref.getEnumTypeList(), fd_new.getEnumTypeList());
        diffServices(fd_ref.getServiceList(), fd_new.getServiceList());


    }

    private void diffServices(List<DescriptorProtos.ServiceDescriptorProto> s_ref, List<DescriptorProtos.ServiceDescriptorProto> s_new) {

    }

    private void diffEnumTypes(List<DescriptorProtos.EnumDescriptorProto> e_ref, List<DescriptorProtos.EnumDescriptorProto> e_new) {

    }

    private Map<String, DescriptorProtos.DescriptorProto> toMap4Descriptor(List<DescriptorProtos.DescriptorProto> in) {
        Map<String, DescriptorProtos.DescriptorProto> out = new HashMap<>();
        in.forEach(descriptor -> {
            out.put(descriptor.getName(), descriptor);
        });
        return out;
    }

    private Set<String> onlyInLeft(Map<String, ?> left, Map<String, ?> right) {
        HashSet<String> strings = new HashSet<>(left.keySet());
        strings.removeAll(right.keySet());
        return strings;
    }

    private Set<String> onlyInCommon(Map<String, ?> left, Map<String, ?> right) {
        Set<String> intersect = new HashSet<>(left.keySet());
        intersect.addAll(right.keySet());

        intersect.removeAll(onlyInLeft(left, right));
        intersect.removeAll(onlyInLeft(right, left));
        return intersect;
    }


    private void diffMessageTypes(List<DescriptorProtos.DescriptorProto> mt_ref, List<DescriptorProtos.DescriptorProto> mt_new) {
        Map<String, DescriptorProtos.DescriptorProto> m_ref = toMap4Descriptor(mt_ref);
        Map<String, DescriptorProtos.DescriptorProto> m_new = toMap4Descriptor(mt_new);

        Set<String> onlyRef = onlyInLeft(m_ref, m_new);
        Set<String> onlyNew = onlyInLeft(m_new, m_ref);
        Set<String> common = onlyInCommon(m_new, m_ref);

        common.forEach(k -> {
            diffMessageType(m_ref.get(k), m_new.get(k));
        });
    }

    private Map<String, DescriptorProtos.FieldDescriptorProto> toMap4FieldDescriptor(List<DescriptorProtos.FieldDescriptorProto> in) {
        Map<String, DescriptorProtos.FieldDescriptorProto> out = new HashMap<>();
        in.forEach(descriptor -> {
            out.put(descriptor.getName(), descriptor);
        });
        return out;
    }


    private void diffMessageType(DescriptorProtos.DescriptorProto d_ref, DescriptorProtos.DescriptorProto d_new) {
        diffFields(d_ref.getFieldList(), d_new.getFieldList());
    }

    private void diffFields(List<DescriptorProtos.FieldDescriptorProto> f_ref, List<DescriptorProtos.FieldDescriptorProto> f_new) {
        Map<String, DescriptorProtos.FieldDescriptorProto> m_ref = toMap4FieldDescriptor(f_ref);
        Map<String, DescriptorProtos.FieldDescriptorProto> m_new = toMap4FieldDescriptor(f_new);

        Set<String> onlyRef = onlyInLeft(m_ref, m_new);
        Set<String> onlyNew = onlyInLeft(m_new, m_ref);
        Set<String> common = onlyInCommon(m_new, m_ref);

        common.forEach(k -> {
            diffField(m_ref.get(k), m_new.get(k));
        });
    }

    private void diffField(DescriptorProtos.FieldDescriptorProto f_ref, DescriptorProtos.FieldDescriptorProto f_new) {

        if (f_ref.equals(f_new)) {
            System.out.println("equal");
        } else {
            System.out.println("NOT");
        }

    }
}
