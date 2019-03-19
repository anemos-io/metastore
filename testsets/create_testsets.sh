#!/usr/bin/env bash

protoc \
 -Itestsets/base \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=core/src/test/resources/io/anemos/metastore/core/proto/base.pb \
 test/v1/simple.proto

protoc \
 -Itestsets/base_deprecate_string \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=core/src/test/resources/io/anemos/metastore/core/proto/base_deprecate_string.pb \
 test/v1/simple.proto

protoc \
 -Itestsets/base_remove_string \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=core/src/test/resources/io/anemos/metastore/core/proto/base_remove_string.pb \
 test/v1/simple.proto

protoc \
 -Itestsets/base_reserve_string \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=core/src/test/resources/io/anemos/metastore/core/proto/base_reserve_string.pb \
 test/v1/simple.proto

protoc \
 -Itestsets/base_reserve_string_only_number \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=core/src/test/resources/io/anemos/metastore/core/proto/base_reserve_string_only_number.pb \
 test/v1/simple.proto

