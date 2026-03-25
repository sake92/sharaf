---
title: Sharaf
description: Sharaf - a minimalistic Scala 3 web framework.
pagination:
  enabled: false
---

# {{ page.title }}

Sharaf is a minimalistic Scala 3 web framework.
  
Jump right into:
- [Tutorials](/tutorials) to get you started
- [How-Tos](/howtos) to get answers for some common questions
- [Reference](/reference) to see detailed information
- [Philosophy](/philosophy) to get insights into design decisions

---

## Site Map
- [Tutorials](/tutorials)
  {% for item in site.data.project.tutorials %}- [{{ item.label }}]({{ item.url}})
  {% endfor %}
- [How Tos](/howtos)
  {% for item in site.data.project.howtos %}- [{{ item.label }}]({{ item.url}})
  {% endfor %}
- [Reference](/reference)
  {% for item in site.data.project.references %}- [{{ item.label }}]({{ item.url}})
  {% endfor %}
- [Philosophy](/philosophy)
  {% for item in site.data.project.philosophies %}- [{{ item.label }}]({{ item.url}})
  {% endfor %}







