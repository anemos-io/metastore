```
protoc \
 -Itestsets/test1 \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=tmp/test1.pb \
 testsets/test1/test/v1alpha1/simple.proto
```

```
protoc \
 -Itestsets/test2 \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=tmp/test2.pb \
 testsets/test2/test/v1alpha1/simple.proto
```


not: --include_imports \
     --include_source_info \
