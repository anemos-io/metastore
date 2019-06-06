
Generate 

`ssh-keygen -t rsa -m pem -C "user@example.com"`

Name the key metastore_repo


Register the key with the remote git repo.

`base64 metastore_repo.pub`


Copy to the config file

```yaml
storage:
  providerClass: io.anemos.metastore.provider.LocalFileProvider
  parameters:
    - name: path
      value: /home/user/test/registries
registries:
  - name: default
    git:
      remote: ssh://user@example.com@source.developers.google.com:2022/p/example-project/r/example-repo
      privateKey: LS0tLS1CR...LS0tCg==
      path: /home/user/test/git/default
```