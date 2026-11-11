Control Block

- Root cause: `LowPriValidators.autoderiveNamedTuple` expanded `Names[T]` for top-level named-tuple unions, so existing `Request.*Validated[(id: Int) | (email: String)]` callers failed with `Tuple element types must be known at compile time`.
- Repair: route named-tuple derivation through a private companion macro; emit the existing no-op `dummyValidator` for `OrType` unions, and emit caller-scope `compiletime.summonAll` validators for concrete named tuples. Restored all three existing Playground union-validation callers unchanged.
- Regression coverage: `sharaf-core/test/src-jvm/ba/sake/sharaf/playground.scala:20,31,42` compile coverage; `validson/test/src/ba/sake/validson/ValidsonSuite.scala:118-129` explicit-validator precedence; existing Option/nested named-tuple cases retained.
- Final diff: only `validson/src/ba/sake/validson/Validator.scala` and `validson/test/src/ba/sake/validson/ValidsonSuite.scala` relative to `3c349c2`; `git diff --check` passed.
- `deder exec -t compile`: PASS, 54 modules, 0 errors, 61 warnings; log `/tmp/sharaf-pr100-ci2-compile.log`.
- `deder exec -t test`: PASS, 155 suites, 329 passed, 12 skipped (341 total); log `/tmp/sharaf-pr100-ci2-test.log`.
- No commits or pushes made; parent owns handoff for branch `fix/named-tuple-options` / PR #100.

Implementation notes

The old inline body could not distinguish a concrete named tuple from a top-level union before evaluating `Names[T]`. The new static macro guard checks the dealiased type with `quotes.reflect.OrType`. Union validators intentionally retain the prior no-op fallback; no runtime union branch validation is promised. Concrete named tuples use the same field names, recursive `Validator` lookup, path prefixes, `Option` handling, and caller-defined validator precedence as before. The explicit precedence test catches accidental selection of the generic autoderived validator over a user-provided `Validator[Named]`.

Verification

- Clean focused `deder exec -t testInMemory -m validson-jvm-test-3.9.0`: PASS, 8 tests; log `/tmp/sharaf-pr100-ci2-validson-explicit-clean.log`.
- Focused `deder exec -t compile -m sharaf-core-jvm-test-3.9.0` with all restored union callers: PASS; log `/tmp/sharaf-pr100-ci2-core-private.log`.
- Full terminal compile and test commands were run once on the final state after the production and test edits were complete.
- Worktree remains uncommitted with only the two scoped Validson files modified; generated `.deder` output is ignored.
