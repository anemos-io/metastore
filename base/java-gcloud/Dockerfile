FROM debian AS builder
RUN apt-get update && apt-get install wget -y
RUN mkdir /opt/cdbg &&\
 wget -qO- https://storage.googleapis.com/cloud-debugger/compute-java/debian-wheezy/cdbg_java_agent_gce.tar.gz |\
 tar xvz -C /opt/cdbg
RUN mkdir -p /opt/cprof &&\
 wget -q -O- https://storage.googleapis.com/cloud-profiler/java/latest/profiler_java_agent.tar.gz |\
 tar xzv -C /opt/cprof


FROM gcr.io/distroless/java
COPY --from=builder /opt/cdbg /opt/cdbg
COPY --from=builder /opt/cprof /opt/cprof

