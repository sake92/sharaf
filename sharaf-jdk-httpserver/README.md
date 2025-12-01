# sharaf-jdk-httpserver

A minimal, zero-dependency HTTP server implementation for Sharaf using Java's built-in `com.sun.net.httpserver.HttpServer`.

## Features

✅ **Zero External Dependencies** - Only requires the JDK!
✅ **Minimal Footprint** - Perfect for simple applications and prototyping
✅ **Easy Deployment** - No additional libraries to bundle
✅ **Full Sharaf API** - Compatible with all Sharaf routing and response features

## Installation

### Mill

```scala
ivy"ba.sake::sharaf-jdk-httpserver:${VERSION}"
```

### SBT

```scala
libraryDependencies += "ba.sake" %% "sharaf-jdk-httpserver" % VERSION
```

### Scala CLI

```scala
//> using dep ba.sake::sharaf-jdk-httpserver:${VERSION}
```

## Quick Start

```scala
import ba.sake.sharaf.*
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer

val routes = Routes:
  case GET -> Path("hello", name) =>
    Response.withBody(s"Hello $name")

val server = JdkHttpServerSharafServer("localhost", 8080, routes)
server.start()
```

## Limitations

Since the JDK HTTP Server is minimal by design, some features have limitations:

### Not Yet Implemented

- **File Uploads**: `multipart/form-data` parsing is not yet supported
  - URL-encoded forms (`application/x-www-form-urlencoded`) work fine
  - For file upload support, use `sharaf-undertow`
- **Session Management**: No built-in session support
  - Unlike `sharaf-undertow`, there's no `Session` context available

### Performance

The JDK HTTP Server is not optimized for high-performance production workloads. For production use cases, consider:
- `sharaf-undertow` - Production-ready with excellent performance
- `sharaf-helidon` - Modern, reactive server

## When to Use

**Perfect for:**
- 🚀 Prototyping and development
- 📦 Minimal deployments
- 🎓 Learning and examples
- 🧪 Testing
- 📱 Simple REST APIs without file uploads

**Not recommended for:**
- ❌ Production systems with high traffic
- ❌ Applications requiring file uploads
- ❌ Applications requiring session management

## API

### Server Creation

```scala
// With custom configuration
JdkHttpServerSharafServer(
  host = "localhost",
  port = 8080,
  routes = routes,
  corsSettings = CorsSettings.default,
  exceptionMapper = ExceptionMapper.default,
  notFoundHandler = _ => Response.withBody("Not Found").withStatus(StatusCode.NotFound)
)

// With just routes
JdkHttpServerSharafServer("localhost", 8080, routes)

// With custom handler
JdkHttpServerSharafServer("localhost", 8080, sharafHandler)

// With custom JDK HTTP handler
JdkHttpServerSharafServer("localhost", 8080, sharafJdkHttpHandler)
```

### Accessing Underlying Request

```scala
import ba.sake.sharaf.jdkhttp.*

val routes = Routes:
  case GET -> Path("info") =>
    val jdkRequest = Request.current.underlying
    val httpExchange = jdkRequest.underlyingHttpExchange
    // Access com.sun.net.httpserver.HttpExchange directly
    Response.withBody(s"Remote address: ${httpExchange.getRemoteAddress}")
```

## Thread Pool Configuration

The default implementation uses a fixed thread pool of 10 threads. To customize:

```scala
val server = com.sun.net.httpserver.HttpServer.create(
  new InetSocketAddress("localhost", 8080),
  0
)
server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(20))
server.createContext("/", sharafJdkHttpHandler)
server.start()
```

## Comparison with Other Implementations

| Feature | jdk-httpserver | undertow | helidon |
|---------|---------------|----------|---------|
| External Dependencies | ✅ None | ❌ Undertow | ❌ Helidon |
| File Uploads | ❌ No | ✅ Yes | ⚠️ TODO |
| Session Support | ❌ No | ✅ Yes | ❌ No |
| Performance | ⚠️ Basic | ✅ Excellent | ✅ Good |
| Production Ready | ⚠️ Simple use cases | ✅ Yes | ✅ Yes |

## Examples

See [examples/jdk-httpserver](../examples/jdk-httpserver/) for working examples.

## Architecture

The implementation follows the same adapter pattern as other Sharaf server modules:

- `JdkHttpServerSharafServer` - Server bootstrap and lifecycle
- `SharafJdkHttpHandler` - Adapts Sharaf's `SharafHandler` to JDK's `HttpHandler`
- `JdkHttpServerSharafRequest` - Implements Sharaf's `Request` trait
- `ResponseUtils` - Converts Sharaf's `Response` to JDK HTTP format
- `CookieUtils` - Cookie conversion utilities
- `JdkHttpExceptionHandler` - Exception handling wrapper

## Contributing

When adding features, please ensure they:
1. Don't introduce external dependencies (except sharaf-core)
2. Follow the existing adapter pattern
3. Include tests
4. Update this README with any limitations

## License

Same as the parent Sharaf project - Apache 2.0
