package ba.sake.sharaf

import io.undertow.util.HttpString
import ba.sake.querson.RawQueryString

type RequestParams = (HttpString, Path, RawQueryString)

type Routes = Request ?=> PartialFunction[RequestParams, Response[?]]
