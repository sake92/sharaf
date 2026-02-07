---
title: Tests
description: Sharaf Tutorial Tests
---

# {{ page.title }}

Tests are essential to any serious software component.  
Writing integration tests with Munit and Requests is straightforward.

Here we are testing the API from the [JSON API tutorial](/tutorials/json.html#routes-definition).  
Create a file `json_api.test.scala` and paste this code into it:
```scala
{% include "json_api.test.scala" %}
```

First run the API server in one shell:
```sh
scala test json_api.sc
```

and then run the tests in another shell:
```sh
scala test json_api.test.scala
```

{% include "tutorial_nav.html" %}

