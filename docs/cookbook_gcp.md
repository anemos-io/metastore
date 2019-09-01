
[https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity]



```yaml
kind: Deployment
apiVersion: apps/v1
metadata:
  name: metastore
spec:
  replicas: 1
  strategy: Recreate
  selector:
    matchLabels:
      app: metastore
  template:
    metadata:
      labels:
        app: metastore
    spec:
      containers:
        - name: metastore
          image: "metastore/metastore:latest"
```


kubectl create configmap metastore --from-file metastore-config.yaml

