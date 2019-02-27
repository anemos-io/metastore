package io.anemos.metastore;

import io.anemos.metastore.core.proto.ProtoDescriptor;

import java.io.IOException;
import java.util.logging.Logger;

public class MetaStore {
    private static final Logger logger = Logger.getLogger(MetaStore.class.getName());
    //    MonoRegistry registry;
    public ProtoDescriptor test;

    /**
     * Create a RouteGuide server listening on {@code port} using {@code featureFile} database.
     */
    public MetaStore() throws IOException {

        test = new ProtoDescriptor("tmp/test1.pb");
//        registry = new MonoRegistry();
    }


}