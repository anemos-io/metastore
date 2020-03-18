FROM debian:stretch AS builder
RUN apt update && apt install curl -y && apt install unzip -y
WORKDIR build
RUN curl https://github.com/protocolbuffers/protobuf/releases/download/v3.11.4/protoc-3.11.4-linux-x86_64.zip -o protoc.zip -L -s
RUN unzip protoc.zip
#
#git clone https://github.com/googleapis/googleapis.git
#git clone https://github.com/grpc/grpc.git
#mv bin ..
#mv include ..
#
#cp -R grpc/src/proto/grpc ../include
#
#mkdir ../include/google/api
#mkdir ../include/google/rpc
#mkdir ../include/google/type
#
#cp ./googleapis/google/api/*.proto ../include/google/api/
#cp ./googleapis/google/rpc/*.proto ../include/google/rpc/
#cp ./googleapis/google/type/*.proto ../include/google/type/
#
#cd ..
#rm -rf tmp
#rm -rf include/grpc/testing/


FROM gcr.io/distroless/java

COPY --from=builder /build/bin/protoc /usr/bin/protoc
COPY --from=builder /build/include /usr/include

