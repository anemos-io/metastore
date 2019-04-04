package io.anemos.metastore.core.proto;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import io.anemos.Bigquery;
import io.anemos.Meta;
import io.anemos.Options;
import io.anemos.Rewrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Convert {

    public static Map<String, DescriptorProtos.FileDescriptorProto> extractProtoMap(DescriptorProtos.FileDescriptorSet fileDescriptorSet) {
        HashMap<String, DescriptorProtos.FileDescriptorProto> map = new HashMap<>();
        fileDescriptorSet.getFileList().forEach(fdp -> map.put(fdp.getName(), fdp));
        return map;
    }

    private static Descriptors.FileDescriptor convertToFileDescriptorMap(String name, Map<String, DescriptorProtos.FileDescriptorProto> inMap, Map<String, Descriptors.FileDescriptor> outMap, ExtensionRegistry extensionRegistry) {
        if (outMap.containsKey(name)) {
            return outMap.get(name);
        }
        DescriptorProtos.FileDescriptorProto fileDescriptorProto = inMap.get(name);
        List<Descriptors.FileDescriptor> dependencies = new ArrayList<>();
        if (fileDescriptorProto.getDependencyCount() > 0) {
            fileDescriptorProto.getDependencyList().forEach(dependencyName ->
                    dependencies.add(convertToFileDescriptorMap(dependencyName, inMap, outMap, extensionRegistry)));
        }
        try {
            Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, dependencies.toArray(new Descriptors.FileDescriptor[0]));
            outMap.put(name, fileDescriptor);
            return fileDescriptor;
        } catch (Descriptors.DescriptorValidationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Descriptors.FileDescriptor> convertFileDescriptorSet(DescriptorProtos.FileDescriptorSet fileDescriptorSet) {
        Map<String, DescriptorProtos.FileDescriptorProto> inMap = extractProtoMap(fileDescriptorSet);
        Map<String, Descriptors.FileDescriptor> outMap = new HashMap<>();
        ExtensionRegistry registry = ExtensionRegistry.newInstance();
        inMap.forEach((k, v) -> convertToFileDescriptorMap(k, inMap, outMap, registry));

        //TODO Find way to do this dynamically
        Options.registerAllExtensions(registry);
        Rewrite.registerAllExtensions(registry);
        Meta.registerAllExtensions(registry);
        Bigquery.registerAllExtensions(registry);

        outMap.forEach( (k, fileDescriptor) -> {
            Descriptors.FileDescriptor.internalUpdateFileDescriptor(fileDescriptor, registry);
        });

        return outMap;
    }

}
