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

protoc \
 -Itestsets/base_add_file \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=core/src/test/resources/io/anemos/metastore/core/proto/base_add_file.pb \
 test/v1/simple.proto \
 test/v1/extra.proto

protoc \
 -Itestsets/base_known_option \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=core/src/test/resources/io/anemos/metastore/core/proto/base_known_option.pb \
 test/v1/simple.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_add_field_option \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=core/src/test/resources/io/anemos/metastore/core/proto/base_add_field_option.pb \
 test/v1/simple.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_change_field_option \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=core/src/test/resources/io/anemos/metastore/core/proto/base_change_field_option.pb \
 test/v1/simple.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_add_message_option \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=core/src/test/resources/io/anemos/metastore/core/proto/base_add_message_option.pb \
 test/v1/simple.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_change_message_option \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=core/src/test/resources/io/anemos/metastore/core/proto/base_change_message_option.pb \
 test/v1/simple.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_add_file_option \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=core/src/test/resources/io/anemos/metastore/core/proto/base_add_file_option.pb \
 test/v1/simple.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_change_file_option \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=core/src/test/resources/io/anemos/metastore/core/proto/base_change_file_option.pb \
 test/v1/simple.proto \
 test/v1/option.proto




