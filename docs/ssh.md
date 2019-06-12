# SSH

## Generate your SSH keys

Setup your remote git repository system for remote SSH access. You need at 
least a SSH key pair, this can be done by the local ssh tools, make sure 
to select the legacy PEM format.

`ssh-keygen -t rsa -m pem -C "user@example.com"`

Name the key metastore_repo (the name is not impartant, but just used as an
example). Register the key with the remote git repo. Refer to the 
documentation of your provider for this.

Maybe you need to configure your local system to associate the key with the 
remote system, here is an example:

```
Host source.developers.google.com
    HostName source.developers.google.com
    User user@exampel.com
    IdentityFile ~/.ssh/metastore_repo
```

This will be needed to collect the remote host information via a git clone.

## Gather information about your SSH setup

Now gather the information to put in the metastore config file. Start with the
public key. Get the base64 of the key and put it in the config file (next section).

`base64 metastore_repo.pub`

Next, get the host infomation to get the remote host information. Execute a clone.

```bash
>>> git clone ssh://user@example.com@source.developers.google.com:2022/p/example-project/r/example-repo
Cloning into 'metastore-test-shadow'...
The authenticity of host '[source.developers.google.com]:2022 ([108.177.126.82]:2022)' can't be established.
ECDSA key fingerprint is SHA256:AGvEpqYNMqsRNIviwyk4J4HM0lEylomDBKOWZsBn434.
Are you sure you want to continue connecting (yes/no)? yes
```

Note the host, key and type from the message and fill it in the configuration file.

## Add the information to the config file

The impoortant part of the config is the git section. You can fill in the **base64**
version of the key in `privateKey`, this is a global key that will be used if no
`privateKey` in the `registries` section is filled in.

For the rest fill in the `hosts` section for each host. You got the information when
you did the initial clone.

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
      path: /home/user/test/git/default
git:
  privateKey: LS0tLS1CR...LS0tCg==
  hosts:
    - host: "[source.developers.google.com]:2022"
      key: AGvEpqYNMqs...ZsBn434
      type: SHA256
```