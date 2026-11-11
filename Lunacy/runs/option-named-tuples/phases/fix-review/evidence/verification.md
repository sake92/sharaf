# Fix-review evidence

## Expected behavior table

| Behavior | Expected result | Regression |
| --- | --- | --- |
| Named-tuple union, left optional field absent and right supplied | Select right arm | Querson/Formson union tests |
| Same union with empty input | Preserve left arm default `None` | Querson/Formson union tests |
| Same union with both fields supplied | Preserve declaration-order left arm | Querson/Formson union tests |
| `Option[NamedTuple]` with unbracketed object serialization | Parse present object; preserve `.field` error paths | Querson/Formson no-bracket tests |
| Abstract `T <: AnyNamedTuple` `.validate` | Compile and retain empty fallback | Validson generic test |
| Concrete named tuple / `Some` invalid nested value | Recursive validation and paths unchanged | Existing Validson tests |

## Red evidence reused

Review reproductions in `phases/review/review-report.md` are the authoritative red probes: union default selection failed in both codecs; unbracketed structured `Option` failed in both codecs; abstract generic named-tuple validation failed during Validson test-source compilation.

## Verification

- `deder exec -t testInMemory -m querson-jvm-test-3.9.0`: PASS, 4 suites / 24 tests.
- `deder exec -t testInMemory -m formson-jvm-test-3.9.0`: PASS, 4 suites / 24 tests.
- `deder exec -t testInMemory -m validson-jvm-test-3.9.0`: PASS, 1 suite / 9 tests.
- `deder exec -t test -m querson-js-test-3.9.0`: PASS, 24 tests.
- `deder exec -t test -m validson-js-test-3.9.0`: PASS, 9 tests. Formson JS is disabled by `deder.pkl`.
- `deder exec -t test -m querson-native-test-3.9.0`: PASS, 24 tests.
- `deder exec -t test -m formson-native-test-3.9.0`: PASS, 24 tests.
- `deder exec -t test -m validson-native-test-3.9.0`: PASS, 9 tests.
- `deder exec -t compile`: PASS, 54 modules, 0 errors, 35 warnings (existing warning families).
- `deder exec -t test`: PASS, 157 suites / 331 passed / 12 skipped.
- `git diff --check`: PASS.
