
You need `clang` for Scala Native: `sudo apt install clang`.

The following steps are done from root of this git repo.
Build the app:

```shell
./mill -i examples.snunit.nativeLink
```


## Run and configure unitd

```shell
sudo unitd --no-daemon --log /dev/stdout --control unix:control.sock
```

Make `config.json` in with this content:

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
      "executable": "out/examples/snunit/nativeLink.dest/out"
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



