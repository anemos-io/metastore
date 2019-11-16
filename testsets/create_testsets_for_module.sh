#!/usr/bin/env bash

MODULE=$1

mkdir -p $MODULE/src/test/resources/io/anemos/metastore/$MODULE

protoc \
 -Itestsets/base \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base.pb \
 test/v1/simple.proto

protoc \
 -Itestsets/base_deprecate_string \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_deprecate_string.pb \
 test/v1/simple.proto

protoc \
 -Itestsets/base_remove_string \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_remove_string.pb \
 test/v1/simple.proto

protoc \
 -Itestsets/base_reserve_string \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_reserve_string.pb \
 test/v1/simple.proto

protoc \
 -Itestsets/base_reserve_string_only_number \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_reserve_string_only_number.pb \
 test/v1/simple.proto

protoc \
 -Itestsets/base_add_file \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_add_file.pb \
 test/v1/simple.proto \
 test/v1/extra.proto

protoc \
 -Itestsets/base_known_option \
 -Itestsets/include \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_known_option.pb \
 test/v1/simple.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_known_option_add_field \
 -Itestsets/include \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_known_option_add_field.pb \
 test/v1/simple.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_add_field_option \
 -Itestsets/include \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_add_field_option.pb \
 test/v1/simple.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_change_field_option \
 -Itestsets/include \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_change_field_option.pb \
 test/v1/simple.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_add_message_option \
 -Itestsets/include \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_add_message_option.pb \
 test/v1/simple.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_change_message_option \
 -Itestsets/include \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_change_message_option.pb \
 test/v1/simple.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_add_file_option \
 -Itestsets/include \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_add_file_option.pb \
 test/v1/simple.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_change_file_option \
 -Itestsets/include \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_change_file_option.pb \
 test/v1/simple.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_complex_message_options \
 -Itestsets/include \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_complex_message_options.pb \
 test/v1/proto3_file.proto \
 test/v1/proto3_message.proto \
 test/v1/proto3_enum.proto \
 test/v1/proto3_service.proto \
 test/v1/proto3_nested.proto \
 test/v1/option.proto

protoc \
 -Itestsets/base_multiple_options \
 -Itestsets/include \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/base_multiple_options.pb \
 test/v1/simple.proto \
 test/v1/option.proto


protoc \
 -Itestsets/shadow_default_field_added \
 -Itestsets/include \
 -I$GOOGLEAPIS_DIR \
 -I$PROTOBUFFER_DIR \
 --descriptor_set_out=$MODULE/src/test/resources/io/anemos/metastore/$MODULE/shadow_default_field_added.pb \
 test/v1/simple.proto \
 test/v1/option.proto
