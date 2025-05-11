package ba.sake.sharaf

enum HttpMethod(val name: String) {
  case GET extends HttpMethod("GET")
  case POST extends HttpMethod("POST")
  case PUT extends HttpMethod("PUT")
  case DELETE extends HttpMethod("DELETE")
  case OPTIONS extends HttpMethod("OPTIONS")
  case PATCH extends HttpMethod("PATCH")
  case HEAD extends HttpMethod("HEAD")
}
