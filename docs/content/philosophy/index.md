---
title: Philosophy
description: Sharaf Philosophy
pagination:
  enabled: false
---

# {{ page.title }}

{% for p in site.data.project.philosophies %}- [{{ p.label }}]({{ p.url}})
{% endfor %}
