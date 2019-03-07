mkdir tmp
cd tmp

curl https://github.com/protocolbuffers/protobuf/releases/download/v3.6.1/protoc-3.6.1-linux-x86_64.zip -o protoc.zip -L -s
unzip protoc.zip

git clone https://github.com/googleapis/googleapis.git
git clone https://github.com/grpc/grpc.git
mv bin ..
mv include ..

cp -R grpc/src/proto/grpc ../include

mkdir ../include/google/api
mkdir ../include/google/rpc
mkdir ../include/google/type

cp ./googleapis/google/api/*.proto ../include/google/api/
cp ./googleapis/google/rpc/*.proto ../include/google/rpc/
cp ./googleapis/google/type/*.proto ../include/google/type/

cd ..
rm -rf tmp