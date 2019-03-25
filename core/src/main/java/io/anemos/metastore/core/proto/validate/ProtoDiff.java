package io.anemos.metastore.core.proto.validate;

import com.google.protobuf.Descriptors;
import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.v1alpha1.ChangeInfo;
import io.anemos.metastore.v1alpha1.ChangeType;
import io.anemos.metastore.v1alpha1.FieldChangeInfo;

import javax.annotation.Nullable;
import java.util.*;

import static io.anemos.metastore.v1alpha1.FieldChangeInfo.FieldChangeType.*;

/**
 * File in package -> can be removed/added/keep the same
 * Message -> can move from file/added/removed/keep the same
 * Message content (fields) -> can keep the same/added/removed
 */
public class ProtoDiff {

    private ProtoDescriptor proto_ref;
    private ProtoDescriptor proto_new;
    private ValidationResults results;

    public ProtoDiff(ProtoDescriptor fd_ref, ProtoDescriptor fd_new, ValidationResults results) {
        this.proto_ref = fd_ref;
        this.proto_new = fd_new;
        this.results = results;
    }

    public void diffOnFileName(String fileName) {
        Descriptors.FileDescriptor fdRef = proto_ref.getFileDescriptorByFileName(fileName);
        Descriptors.FileDescriptor fdNew = proto_new.getFileDescriptorByFileName(fileName);
        diffFileDescriptor(fdRef, fdNew);
    }

    private void diffFileDescriptor(@Nullable Descriptors.FileDescriptor fdRef, @Nullable Descriptors.FileDescriptor fdNew) {
        List<Descriptors.Descriptor> refDescriptors;
        List<Descriptors.EnumDescriptor> refEnumDescriptors;
        List<Descriptors.ServiceDescriptor> refServiceDescriptors;
        List<Descriptors.Descriptor> newDescriptors;
        List<Descriptors.EnumDescriptor> newEnumDescriptors;
        List<Descriptors.ServiceDescriptor> newServiceDescriptors;
        if (fdRef != null) {
            refDescriptors = fdRef.getMessageTypes();
            refEnumDescriptors = fdRef.getEnumTypes();
            refServiceDescriptors = fdRef.getServices();
        } else {
            refDescriptors = new ArrayList<>(0);
            refEnumDescriptors = new ArrayList<>(0);
            refServiceDescriptors = new ArrayList<>(0);
        }

        if (fdNew != null) {
            newDescriptors = fdNew.getMessageTypes();
            newEnumDescriptors = fdNew.getEnumTypes();
            newServiceDescriptors = fdNew.getServices();
        } else {
            newDescriptors = new ArrayList<>(0);
            newEnumDescriptors = new ArrayList<>(0);
            newServiceDescriptors = new ArrayList<>(0);
        }

        diffMessageTypes(refDescriptors, newDescriptors);
        diffEnumTypes(refEnumDescriptors, newEnumDescriptors);
        diffServices(refServiceDescriptors, newServiceDescriptors);
    }

    public void diffOnMessage(String messageName) {
        Descriptors.Descriptor refDescriptor = proto_ref.getDescriptorByName(messageName);
        Descriptors.Descriptor newDescriptor = proto_new.getDescriptorByName(messageName);

        diffMessageType(refDescriptor, newDescriptor);
    }

    public void diffOnPackagePrefix(String packagePrefix) {
        List<Descriptors.FileDescriptor> fdRef = proto_ref.getFileDescriptorsByPackagePrefix(packagePrefix);
        List<Descriptors.FileDescriptor> fdNew = proto_new.getFileDescriptorsByPackagePrefix(packagePrefix);
        diffFiles(fdRef, fdNew);
    }

    private void diffServices(List<Descriptors.ServiceDescriptor> s_ref, List<Descriptors.ServiceDescriptor> s_new) {

    }

    private void diffEnumTypes(List<Descriptors.EnumDescriptor> e_ref, List<Descriptors.EnumDescriptor> e_new) {

    }

    static Map<String, Descriptors.FileDescriptor> toMap4FileDescriptor(List<Descriptors.FileDescriptor> in) {
        Map<String, Descriptors.FileDescriptor> out = new HashMap<>();
        in.forEach(descriptor -> {
            out.put(descriptor.getName(), descriptor);
        });
        return out;
    }

    private Map<String, Descriptors.Descriptor> toMap4Descriptor(List<Descriptors.Descriptor> in) {
        Map<String, Descriptors.Descriptor> out = new HashMap<>();
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


    private void diffMessageTypes(List<Descriptors.Descriptor> mt_ref, List<Descriptors.Descriptor> mt_new) {
        Map<String, Descriptors.Descriptor> m_ref = toMap4Descriptor(mt_ref);
        Map<String, Descriptors.Descriptor> m_new = toMap4Descriptor(mt_new);

        Set<String> onlyRef = onlyInLeft(m_ref, m_new);
        onlyRef.forEach(k -> {
            Descriptors.Descriptor fd = m_ref.get(k);
            results.setPatch(fd, ChangeInfo.newBuilder()
                    .setChangeType(ChangeType.REMOVAL)
                    .setFromName(fd.getFullName())
                    .build());
        });

        Set<String> onlyNew = onlyInLeft(m_new, m_ref);
        onlyNew.forEach(k -> {
            Descriptors.Descriptor fd = m_new.get(k);
            results.setPatch(fd, ChangeInfo.newBuilder()
                    .setChangeType(ChangeType.ADDITION)
                    .setFromName(fd.getFullName())
                    .build());
        });

        Set<String> common = onlyInCommon(m_new, m_ref);
        common.forEach(k -> {
            diffMessageType(m_ref.get(k), m_new.get(k));
        });
    }

    static Map<String, Descriptors.FieldDescriptor> toMap4FieldDescriptor(List<Descriptors.FieldDescriptor> in) {
        Map<String, Descriptors.FieldDescriptor> out = new HashMap<>();
        in.forEach(descriptor -> {
            out.put(String.valueOf(descriptor.getNumber()), descriptor);
        });
        return out;
    }

    private void diffFiles(List<Descriptors.FileDescriptor> f_ref, List<Descriptors.FileDescriptor> f_new) {
        Map<String, Descriptors.FileDescriptor> m_ref = toMap4FileDescriptor(f_ref);
        Map<String, Descriptors.FileDescriptor> m_new = toMap4FileDescriptor(f_new);

        Set<String> onlyRef = onlyInLeft(m_ref, m_new);
        onlyRef.forEach(k -> {
            Descriptors.FileDescriptor fd = m_ref.get(k);
            results.setPatch(fd, ChangeInfo.newBuilder()
                    .setChangeType(ChangeType.REMOVAL)
                    .setFromName(fd.getName())
                    .build());
            diffFileDescriptor(fd, null);
        });

        Set<String> onlyNew = onlyInLeft(m_new, m_ref);
        onlyNew.forEach(k -> {
            Descriptors.FileDescriptor fd = m_new.get(k);
            results.setPatch(fd, ChangeInfo.newBuilder()
                    .setChangeType(ChangeType.ADDITION)
                    .setToName(fd.getName())
                    .build());
            diffFileDescriptor(null, fd);
        });

        Set<String> common = onlyInCommon(m_new, m_ref);
        common.forEach(k -> {
            diffFileDescriptor(m_ref.get(k), m_new.get(k));
        });
    }

    private void diffMessageType(Descriptors.Descriptor d_ref, Descriptors.Descriptor d_new) {
        diffFields(d_ref, d_new);
    }

    private void diffFields(Descriptors.Descriptor d_ref, Descriptors.Descriptor d_new) {
        Map<String, Descriptors.FieldDescriptor> m_ref = toMap4FieldDescriptor(d_ref.getFields());
        Map<String, Descriptors.FieldDescriptor> m_new = toMap4FieldDescriptor(d_new.getFields());

        Set<String> onlyRef = onlyInLeft(m_ref, m_new);
        onlyRef.forEach(k -> {
            Descriptors.FieldDescriptor fd = m_ref.get(k);
            FieldChangeInfo.Builder builder = FieldChangeInfo.newBuilder()
                    .setChangeType(FIELD_REMOVED)
                    .setFromName(fd.getName())
                    .setFromTypeValue(fd.getType().toProto().getNumber())
                    .setFromDeprecated(isDeprecated(fd));
            if (d_new.isReservedNumber(fd.getNumber())) {
                builder.setChangeType(FIELD_RESERVED);
                if (d_new.isReservedName(fd.getName())) {
                    builder.setToName(fd.getName());
                }
            }
            results.setPatch(fd, builder.build());
        });

        Set<String> onlyNew = onlyInLeft(m_new, m_ref);
        onlyNew.forEach(k -> {
            Descriptors.FieldDescriptor fd = m_new.get(k);
            FieldChangeInfo.Builder builder = FieldChangeInfo.newBuilder()
                    .setChangeType(FIELD_ADDED)
                    .setToName(fd.getName())
                    .setToTypeValue(fd.getType().toProto().getNumber())
                    .setToDeprecated(isDeprecated(fd));
            if (d_ref.isReservedNumber(fd.getNumber())) {
                builder.setChangeType(FIELD_UNRESERVED);
                if (d_ref.isReservedName(fd.getName())) {
                    builder.setFromName(fd.getName());
                }
            }
            results.setPatch(fd, builder.build());
        });

        Set<String> common = onlyInCommon(m_new, m_ref);
        common.forEach(k -> {
            FieldChangeInfo fieldDiff = diffField(m_ref.get(k), m_new.get(k));
            if (fieldDiff != null) {
                results.setPatch(m_ref.get(k), fieldDiff);
            }
        });
    }

    private FieldChangeInfo diffField(Descriptors.FieldDescriptor f_ref, Descriptors.FieldDescriptor f_new) {
        FieldChangeInfo.Builder builder = FieldChangeInfo.newBuilder();

        if (!f_ref.getName().equals(f_new.getName())) {
            builder.setChangeType(FIELD_CHANGED);
            builder.setFromName(f_ref.getName());
            builder.setToName(f_new.getName());
        }
        if (!f_ref.getType().equals(f_new.getType())) {
            builder.setChangeType(FIELD_CHANGED);
            builder.setFromTypeValue(f_ref.getType().toProto().getNumber());
            builder.setToTypeValue(f_new.getType().toProto().getNumber());
        }
        if(isDeprecated(f_ref) != isDeprecated(f_new)) {
            builder.setChangeType(FIELD_CHANGED);
            builder.setFromDeprecated(isDeprecated(f_ref));
            builder.setToDeprecated(isDeprecated(f_new));
        }

        if (builder.getChangeType().equals(FIELD_CHANGED)) {
            return builder.build();
        }
        return null;
    }

    private boolean isDeprecated(Descriptors.FieldDescriptor fieldDescriptor) {
        Map<Descriptors.FieldDescriptor, Object> allFields = fieldDescriptor.getOptions().getAllFields();
        if (allFields.size() > 0) {
            for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : allFields.entrySet()) {
                Descriptors.FieldDescriptor f = entry.getKey();
                switch (f.getFullName()) {
                    case "google.protobuf.FieldOptions.deprecated":
                        return true;
                }
            }
        }
        return false;
    }
}
