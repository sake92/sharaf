---
title: Reference
description: Sharaf Reference
pagination:
  enabled: false
---

# {{ page.title }}

{% for ref in site.data.project.references %}- [{{ ref.label }}]({{ ref.url}})
{% endfor %}
