
You need a Scala Native C toolchain (`clang`, preferably version 16 or newer), NGINX Unit (`unitd`),
and its development library (`libunit`). The linker must be able to locate `libunit`; otherwise the build fails
with `cannot find -lunit`. Follow the [Unit installation instructions](https://unit.nginx.org/installation/)
for your platform, including development files and support for external applications.

These instructions describe this repository's pinned snunit 0.10.3 and external-Unit setup.
[NGINX Unit is archived](https://unit.nginx.org/); newer snunit versions use [FreeUnit](https://github.com/lolgab/snunit)
and have a different packaging workflow.

The following steps are done from root of this git repo.
Build the app:

```shell

deder exec -t nativeLink -m examples-snunit

```


## Run and configure unitd

```shell
sudo unitd --no-daemon --log /dev/stdout --control unix:control.sock
```

Make `config.json` with this content. Replace `/absolute/path/to/sharaf` with your repository's absolute path:

```json
{
  "listeners": {
    "*:8081": {
      "pass": "applications/myapp"
    }
  },
  "applications": {
    "myapp": {
      "type": "external",
      "executable": "/absolute/path/to/sharaf/.deder/out/examples-snunit/nativeLink/native/main"
    }
  }
}
```

Then in another shell:

```shell
curl -X PUT --unix-socket control.sock -d @config.json localhost/config

curl localhost:8081
# should return "Hello Snunit!"
```



