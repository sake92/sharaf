---
title: Static Files
description: Sharaf Tutorial Static Files
---

# {{ page.title }}

The static files are automatically served from the `resources/public` folder (on the classpath):
- in Mill those are under `my_project/resources/public`
- in sbt those are under `src/main/resources/public`
- in scala you need to manually tell it where to look for with `--resource-dir resources`

---

Let's serve an `example.js` file with Sharaf.  
First create a file `resources/public/example.js`.  
Put this text into it: `console.log('Hello Sharaf!');`.

Now create a file `static_files.sc` and paste this code into it:
```scala
{% include "static_files.sc" %}
```

and run it like this:
```sh
scala static_files.sc  --resource-dir resources
```

Go to http://localhost:8181/example.js.  
You will see the `example.js` contents served.

