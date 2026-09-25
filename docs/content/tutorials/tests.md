---
layout: tutorial.html
title: Tests
description: Sharaf Tutorial Tests
---

# {{ page.title }}

Tests are essential to any serious software component.  
This example tests the server from the [JSON API tutorial](/tutorials/json.html) with MUnit and sttp client4.

Create a file `json_api.test.scala` and paste this code into it:
```scala
{% include "json_api.test.scala" %}
```

First run the API server in one shell:
```sh
scala json_api.sc
```

and then run the tests in another shell:
```sh
scala test json_api.test.scala
```

