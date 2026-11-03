---
layout: home.html
title: Sharaf
description: A simple Scala 3 web framework. Build web apps and APIs, and deploy on a single VPS or AWS Lambda.
pagination:
  enabled: false
---

<section class="home-hero" aria-labelledby="hero-title">
<div class="home-hero-copy">
<p class="home-eyebrow">Sharaf · Scala 3 web framework</p>
<h1 id="hero-title">Start simple.<br><span>Deploy anywhere.</span></h1>
<p class="home-lead">Build web apps and APIs with readable routes, typed inputs, and ordinary Scala. From your first route to a single VPS or AWS Lambda.</p>
<div class="home-actions">
<a href="/tutorials/quickstart.html" class="home-button home-button-primary">Get started <span aria-hidden="true">→</span></a>
<a href="#deployment" class="home-button">Explore deployments</a>
</div>
<p class="home-platforms">JVM &amp; native executables <span aria-hidden="true">·</span> Open source</p>
</div>
<div class="home-example">
<div class="home-example-title"><span>hello.sc</span><span>A web app in a few lines.</span></div>

```scala
{% include "hello.sc" %}
```

<div class="home-example-run"><span>Run it with Scala CLI</span><code>scala hello.sc</code></div>
</div>
</section>

<section class="home-section" aria-labelledby="why-title">
<p class="home-eyebrow">Start small. Grow as needed.</p>
<h2 id="why-title">Why Sharaf?</h2>
<div class="home-benefits">
<article class="home-card">
<span class="home-card-icon" aria-hidden="true">⚡</span>
<h3>Simple by design</h3>
<p>Routes are Scala pattern matches. Application logic is ordinary Scala code. Start small and keep your app easy to follow.</p>
<a href="/philosophy/why-sharaf.html">The ideas behind Sharaf <span aria-hidden="true">→</span></a>
</article>
<article class="home-card">
<span class="home-card-icon" aria-hidden="true">🎯</span>
<h3>Typed where it matters</h3>
<p>Parse JSON, forms, and query parameters into case classes. Add validation and return useful errors.</p>
<a href="/tutorials/validation.html">Explore typed inputs <span aria-hidden="true">→</span></a>
</article>
<article class="home-card">
<span class="home-card-icon" aria-hidden="true">🧩</span>
<h3>Build the whole app</h3>
<p>JSON APIs, HTML pages, forms, cookies, and sessions—with libraries that fit together.</p>
<a href="/tutorials/">Explore the tutorials <span aria-hidden="true">→</span></a>
</article>
<article class="home-card">
<span class="home-card-icon" aria-hidden="true">🚀</span>
<h3>Choose your deployment</h3>
<p>Run on the JVM or compile a native executable. Use your own server, containers, or AWS Lambda.</p>
<a href="/reference/servers.html">Servers and platforms <span aria-hidden="true">→</span></a>
</article>
</div>
</section>

<section id="deployment" class="home-section home-deployment" aria-labelledby="deployment-title">
<p class="home-eyebrow">Your app. Your infrastructure.</p>
<h2 id="deployment-title">From a single VM to serverless.</h2>
<p class="home-section-intro">Start with the setup that fits your app. These starters show two ways to put Sharaf to work.</p>
<div class="home-deployment-grid">
<article class="home-card">
<p class="home-tag">Your own server</p>
<h3>A small stack you control.</h3>
<p>A Scala Native Sharaf app behind Caddy HTTPS and VoidAuth authentication. Keep the app private behind your proxy and manage the stack with Docker Compose.</p>
<a href="https://github.com/sake92/sharaf-deployment-starter">Explore the VPS starter <span aria-hidden="true">↗</span></a>
</article>
<article class="home-card">
<p class="home-tag">AWS Lambda</p>
<h3>Let the platform handle capacity.</h3>
<p>Run your Sharaf app as a native executable on AWS Lambda. Write ordinary Scala routes and let AWS manage server capacity.</p>
<a href="https://github.com/sake92/sharaf-aws-lambda-starter">Explore the Lambda starter <span aria-hidden="true">↗</span></a>
</article>
</div>
<p class="home-footprint">Considering native? See <a href="/reference/native-footprint.html">measured startup, memory, and executable sizes</a>.</p>
</section>

<section class="home-section home-docs" aria-labelledby="docs-title">
<div>
<p class="home-eyebrow">Keep building</p>
<h2 id="docs-title">Find your next step.</h2>
</div>
<div class="home-doc-links">
<a href="/tutorials/"><strong>Tutorials <span aria-hidden="true">→</span></strong><span>Build your first app, then add more.</span></a>
<a href="/howtos/"><strong>How-to guides <span aria-hidden="true">→</span></strong><span>Solve a specific problem.</span></a>
<a href="/reference/"><strong>Reference <span aria-hidden="true">→</span></strong><span>Look up APIs, servers, and options.</span></a>
<a href="/philosophy/"><strong>Philosophy <span aria-hidden="true">→</span></strong><span>Understand the design decisions.</span></a>
</div>
</section>
