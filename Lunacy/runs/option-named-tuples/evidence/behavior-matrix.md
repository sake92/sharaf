## Behavior matrix

| Surface | present | absent | empty | invalid | write / round trip |
| --- | --- | --- | --- | --- | --- |
| Querson named tuple field `Option[String]` | PASS | FAIL: `QuersonException` missing field | PASS -> `None` | PASS: `ParsingException` | PASS: `opt[0]`; `None` omitted |
| Formson named tuple field `Option[String]` | PASS | FAIL: `ParsingException` `.opt` missing | PASS -> `None` | PASS: `ParsingException` | PASS: `opt[0]`; `None` omitted |
| Querson/Formson `Option[NamedTuple]` field | writer emits indexed object | FAIL on parse: `Option` probes `Seq[String]` | PASS for empty scalar | n/a | no round trip; object sequence is rejected |
| Validson named tuple field `Option[T]` | no-op fallback | no-op fallback | no-op fallback | no-op fallback | `Validator.derived` fails at macro expansion; existing case-class `Option` recursion remains green |

Case-class baselines already cover optional present/empty/absent behavior in `QuerySimple`/`QueryDefaults` and `FormSimple`/`FormDefaults`; existing Validson `OptionalData` covers nested `Option[Seq[T]]`.
