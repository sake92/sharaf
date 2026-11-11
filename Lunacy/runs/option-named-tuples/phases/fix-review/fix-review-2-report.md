# Fix-review follow-up report

## Control Block

- Result: PASS; parent gate boundary regressions are covered in both codec suites.
- Added a multi-field aliased named-tuple union arm and a three-arm union where the third supplied arm must beat two earlier all-default arms.
- Added supplied-first assertions to retain declaration-order behavior when the first aliased arm has matching fields.
- Production behavior is unchanged in this follow-up; only the gate-requested incidental blank-line cleanup remains alongside the test additions.
- Changed tests: `querson/test/src/ba/sake/querson/QueryStringParseSuite.scala:329-340` and `formson/test/src/ba/sake/formson/FormDataParseSuite.scala:367-381`.
- Checks: affected Querson JVM 4 suites / 24 tests passed; affected Formson JVM 4 suites / 24 tests passed; `git diff --check` passed.
- Parent handles commit/push; no commit or push performed.

## Boundary covered

`First = (first: Option[String], marker: Option[Int])`, `Second = (second: Option[String])`, and `Third = (third: String)` exercise recursive union matcher metadata, multiple field-name extraction, first-arm matching, and later-arm selection. Both codecs assert the first arm when `first` and `marker` are supplied and the third arm when only `third` is supplied.
