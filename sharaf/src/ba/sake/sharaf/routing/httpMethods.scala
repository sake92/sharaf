package ba.sake.sharaf.routing

import io.undertow.util.Methods

enum HttpMethod(val name: String) {
  case GET extends HttpMethod(Methods.GET_STRING)
  case POST extends HttpMethod(Methods.POST_STRING)
  case PUT extends HttpMethod(Methods.PUT_STRING)
  case DELETE extends HttpMethod(Methods.DELETE_STRING)
  case OPTIONS extends HttpMethod(Methods.OPTIONS_STRING)
  case PATCH extends HttpMethod(Methods.PATCH_STRING)
}
