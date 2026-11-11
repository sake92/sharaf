# Fix-review report

## Control Block

- Result: PASS; all three confirmed CodeRabbit findings are repaired in `/home/sake/projects/sake92/sharaf/.worktrees/option-named-tuples`.
- Union parsing now prefers the arm with supplied named-tuple fields; absent input still uses the first arm's defaults and ambiguous input keeps declaration order.
- `Option[NamedTuple]` now parses object-shaped values emitted by no-bracket sequence modes in both codecs, including nested error paths.
- Validson derives only concrete named tuples; abstract `T <: AnyNamedTuple` and union fallback remain no-op validators.
- Changed production: `querson/src/ba/sake/querson/QueryStringRW.scala`, `formson/src/ba/sake/formson/FormDataRW.scala`, `validson/src/ba/sake/validson/Validator.scala`.
- Changed tests: corresponding Querson/Formson parse suites and Validson generic-validator regression.
- Full compile: 54 modules, 0 errors. Full test: 157 suites, 331 passed, 12 skipped.
- Parent diff pointers: Querson lines 194-195, 390-455; Formson lines 226-227, 484-549; Validson lines 148-152; tests around Querson 267-327, Formson 294-365, Validson 5-8.
- Evidence: `phases/fix-review/evidence/verification.md`; no commit, push, or GitHub action performed.

## Implementation and limits

The union macro generates a small shape matcher from named-tuple type metadata. It only changes arm selection when the left branch has no supplied named-tuple field and the right branch does; otherwise existing left-first parsing and exception fallback remain intact. Standalone defaults, required-field errors, scalar options, and concrete named-tuple validation are unchanged.

The object branch in each `Option` reader delegates directly to the element reader. Sequence/simple handling remains as before, so absent and empty values still produce `None`, while structured invalid values retain field-qualified errors.

The Validson macro guard checks the concrete named-tuple type symbol before expanding `Names[T]`; abstract generic callers compile through the established `dummyValidator`, while concrete recursive derivation and explicit-validator precedence remain covered by existing tests.

## Checks

See `phases/fix-review/evidence/verification.md` for the complete command matrix. JVM Querson/Formson/Validson, Querson/Validson JS, and Querson/Formson/Validson Native targeted suites passed. Formson JS remains disabled by the repository build configuration. `git diff --check` passed.
