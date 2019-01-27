package io.anemos.metastore.core.proto;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProtoDescriptor {

    private Map<String, Descriptors.FileDescriptor> fileDescriptorMap;
    private Map<String, Descriptors.Descriptor> descriptorMap;


    public ProtoDescriptor(String sd) throws IOException {
        DescriptorProtos.FileDescriptorSet fileDescriptorProto = DescriptorProtos.FileDescriptorSet.parseFrom(new FileInputStream(sd));


        fileDescriptorMap = Convert.convertFileDescriptorSet(fileDescriptorProto);
        indexDescriptorByName();

    }

    public Descriptors.FileDescriptor getFileDescriptorByFileName(String fileName) {
        return fileDescriptorMap.get(fileName);
    }


    public void writeToDirectory(String root) {
        fileDescriptorMap.forEach(
                (k, v) -> {
                    File file = new File(root + k);
                    file.getParentFile().mkdirs();
                    try (OutputStream out = new FileOutputStream(file)) {
                        ProtoLanguageFileWriter.write(v, out);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    System.out.println(k);
                    //ProtoLanguageFileWriter.write(v, System.out);
                }

        );

    }


    private void indexNestedDescriptorByName(List<Descriptors.Descriptor> nestedTypes) {
        nestedTypes.forEach(mt -> {
            descriptorMap.put(mt.getFullName(), mt);
            indexNestedDescriptorByName(mt.getNestedTypes());
        });
    }

    private void indexDescriptorByName() {
        descriptorMap = new HashMap<>();
        fileDescriptorMap.forEach(
                (k, v) -> {
                    v.getMessageTypes().forEach(
                            mt -> {
                                descriptorMap.put(mt.getFullName(), mt);
                                indexNestedDescriptorByName(mt.getNestedTypes());
                            }
                    );
                });
    }

    public Descriptors.Descriptor getDescriptorByName(String messageName) {
        return descriptorMap.get(messageName);
    }
}
