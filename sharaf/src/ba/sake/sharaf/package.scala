package ba.sake.sharaf

import io.undertow.util.HttpString

type RequestParams = (HttpString, Path)

type Routes = Request ?=> PartialFunction[RequestParams, Response[?]]
