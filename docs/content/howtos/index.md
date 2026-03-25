---
layout: howto.html
title: How Tos
description: Sharaf How Tos
pagination:
  enabled: false
---

# {{ page.title }}


Here are some common questions and answers you might have when using Sharaf.

{% for h in site.data.project.howtos %}- [{{ h.label }}]({{ h.url}})
{% endfor %}