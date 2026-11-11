## Control Block

- Result: PASS; all three CodeRabbit inline findings are relevant on current commit `33dbd31`.
- Finding 1 (union arm/defaults): CONFIRMED, introduced by PR, major data-integrity bug; fix in this PR.
- Finding 2 (unbracketed structured `Option`): CONFIRMED, pre-existing but in the PR's advertised Option round-trip surface; fix in this PR or explicitly defer/document.
- Finding 3 (abstract generic named-tuple validator): CONFIRMED, introduced by PR, compile-breaking API regression; fix in this PR.
- Reproduction probes were temporary only and removed. Production source was not changed; no commit/push/GitHub reply.
- Final source worktree: `/home/sake/projects/sake92/sharaf/.worktrees/option-named-tuples`, clean; `git diff --check` PASS.

## Findings

### 1. Union selection accepts a default before matching supplied fields

- Comment: https://github.com/sake92/sharaf/pull/100#discussion_r4447979591
- Current paths: `querson/src/ba/sake/querson/QueryStringRW.scala:394-399,499-504`; Formson equivalent `formson/src/ba/sake/formson/FormDataRW.scala:488-493,590-594`.
- Probe input: `Map("right" -> Seq("value"))` / `SeqMap("right" -> Seq(FormValue.Str("value")))`, target `(left: Option[String]) | (right: String)`.
- Result: both parsers returned `(left = None)` instead of `(right = "value")`; the first named-tuple arm consumes `rw.default = Some(None)` and the union never tries the second arm.
- Command/result: `deder exec -t testInMemory -m querson-jvm-test-3.9.0` and `... formson-jvm-test-3.9.0`; each exit 1, 24 existing tests passed and 1 probe suite had 1 failure. Logs: `/tmp/sharaf-pr100-querson-inmemory.log`, `/tmp/sharaf-pr100-formson-inmemory.log`.
- Introduced by PR: base named-tuple readers threw on the omitted field; the changed default branch now makes the wrong first arm succeed. Scope both Querson and Formson union readers; preserve standalone named-tuple defaults and prefer a supplied-field arm.

### 2. `Option[NamedTuple]` does not round-trip with `withSeqNoBrackets`

- Comment: https://github.com/sake92/sharaf/pull/100#discussion_r4447979601
- Current paths: `querson/src/ba/sake/querson/QueryStringRW.scala:180-198`; Formson has the same path at `formson/src/ba/sake/formson/FormDataRW.scala:212-230`.
- Probe value: `(person = Some((name = "Ada", age = 37)))`; serialization with `DefaultQuersonConfig.withSeqNoBrackets` or `DefaultFormsonConfig.withSeqNoBrackets` emits an object under `person`.
- Result: both parsers throw `ParsingException`: `Key '.person' should be Seq but it is object` / `Form parsing error: Key '.person' should be Seq but it is object`. The `case other` branch still routes objects through `Seq[String]` instead of the element reader.
- Same commands/logs as finding 1; each had the second probe failure. This behavior is pre-existing (the base Option reader had the same object-rejecting fallback), but it is reachable in the newly advertised structured Option round-trip and should be fixed in both codecs if all Config modes are in scope; otherwise document/defer explicitly.

### 3. Generic `T <: AnyNamedTuple` validator helper fails to compile

- Comment: https://github.com/sake92/sharaf/pull/100#discussion_r4447979618
- Current paths: `validson/src/ba/sake/validson/Validator.scala:146-169,174-177`; `constValueTuple[Names[T]]` is expanded for abstract `T` because `autoderiveNamedTuple` has no concrete-tuple guard.
- Probe: `def check[T <: AnyNamedTuple](value: T): Seq[ValidationError] = value.validate`.
- Command/result: `deder exec -t testInMemory -m validson-jvm-test-3.9.0` exits 1 during test-source compilation with `Tuple element types must be known at compile time` at the helper's `.validate`; log: `/tmp/sharaf-pr100-validson-inmemory.log`.
- Introduced by PR: before the new `autoderiveNamedTuple`, `dummyValidator[T]` supplied the fallback and the generic helper compiled. Restrict automatic derivation to concrete named tuples while retaining the existing abstract-type no-op fallback; preserve the current union guard.

## Cleanup / baseline

- Temporary files: `CodeRabbitReviewProbeSuite.scala` in Querson, Formson, and Validson test trees; all removed with `apply_patch`.
- Existing repair baseline remains documented in `phases/repair/repair-option-report.md` and `evidence/verification.md`: affected JVM/JS/Native suites passed before these review probes; no production files were altered during assessment.
