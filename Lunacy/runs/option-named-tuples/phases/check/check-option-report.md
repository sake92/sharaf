## Control Block

- Result: PASS; focused behavioral tests added, no production semantics changed.
- Behavior: Option fields in named tuples parse present/empty/invalid values; absent fields fail because named-tuple parsers bypass `rw.default`.
- Option-wrapped named tuples serialize indexed objects but do not parse back in either codec; Validson named-tuple derivation fails at macro expansion and its fallback is no-op.
- Changed paths: five test files in `/home/sake/projects/sake92/sharaf/.worktrees/option-named-tuples` (Querson 2, Formson 2, Validson 1).
- Verification: Querson 4 suites/24 tests PASS; Formson 4 suites/24 tests PASS; Validson 1 suite/7 tests PASS; `git diff --check` PASS.
- Parent inspection: `git diff -- querson/test/src/ba/sake/querson/{QueryStringParseSuite.scala,QueryStringWriteSuite.scala}` and corresponding Formson/Validson paths; matrix in `evidence/behavior-matrix.md`.
- Evidence: commands and compile-time probe are summarized in `evidence/verification.md`; full logs remain under `/tmp/option-named-tuples-evidence/`.
- Residuals: production fixes require a parent decision; no commit or push made. Next action: parent acceptance and decide whether to repair limitations.

## Findings

Querson and Formson both have low-priority named-tuple derivation that summons the field codec. Their `Option[T]` codecs provide `default = Some(None)`, but named-tuple parsing directly throws when a field is absent, unlike case-class derivation which consults codec defaults. Present values, empty strings, invalid scalar values, and serialization are covered by the new tests.

An `Option[NamedTuple]` field can be written (`person[0].name`, `person[0].age` / Formson bracket form), but parsing fails because the Option codec first parses the sequence as `Seq[String]` to locate a non-empty value. Object elements are rejected before the nested named-tuple codec runs. This is a codec limitation, not a typeclass summon failure.

Validson's `optValidator` recursively validates `Some` and accepts `None` when a nested validator exists. However, `Validator.derived` assumes its product value can be quoted as `Product`; deriving for a named tuple fails with `scala.quoted.runtime.impl.ExprCastException`. Without an explicit validator, `LowPriValidators.dummyValidator` makes a named tuple with an Option field silently no-op. Existing case-class optional recursion remains covered and green.

Request's `queryParams`/`bodyForm` entry points accept `AnyNamedTuple` and delegate to these codecs; no separate request-layer behavior was changed or added.
