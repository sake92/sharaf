package ba.sake.sharaf

import io.undertow.util.HttpString
import ba.sake.querson.QueryStringMap

type RequestParams = (HttpString, Path, QueryStringMap)

type Routes = Request ?=> PartialFunction[RequestParams, Response[?]]
