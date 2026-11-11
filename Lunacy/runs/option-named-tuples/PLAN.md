# Option with named tuples

User request: `$lunacy check if Option works when used with named tuples for querson/formson etc`.

Check existing named-tuple support in Querson, Formson, and Validson and related Sharaf parsing entry points. Verify optional fields with present, absent, empty, and invalid input and serialization/round trips where supported. Compare case-class behavior as appropriate. Investigate Option wrapping a named tuple if the existing public API supports it. Report limitations precisely.

Investigation phase complete. User now authorizes fixes: `can we fix, bugs in macro or?`. Repair all three demonstrated issues: missing optional named-tuple fields, Option wrapping structured named-tuple values in Querson/Formson, and Validson named-tuple derivation/implicit validation. Preserve case-class defaults and existing scalar Option behavior, error paths, and supported platforms. No new dependencies or broad API redesign. Use existing worktree `.worktrees/option-named-tuples`. No commits or pushes requested.

Repair phase: one worker owns all affected implementations and tests, inventories existing mechanics/callers first, replaces characterization tests with positive regression expectations, demonstrates red then green, verifies JVM suites and representative enabled JS/Native targets where available. Include named-tuple validation through normal implicit lookup and explicit derivation, None and Some valid/invalid, nested Option structured cases, and absent/empty/invalid values. Escalate only material scope/API expansion. Parent inspects exact diff regions plus one bounded acceptance check; no duplicate full matrix.

Authority: repository AGENTS.md; lunacy skill and worker/ENGINEERING.md. Load pragmatic-testing for tests, pragmatic-architecture if a production review becomes necessary, and find-docs/Context7 for library API details as applicable.

Verification owner: worker runs affected Deder module tests and writes logs to run evidence. Parent reads terminal Control Block, inspects targeted diff, and performs one bounded acceptance check. No adversary/scout needed.

CI recovery: user reports PR #100 does not compile. Downloaded run 41528076190 log at /tmp/sharaf-pr100-ci-failure.log confirms sharaf-core-jvm-test-3.9.0 compilation has two errors. Existing module-only proof missed dependent consumers. Fix actual root cause and preserve original named-tuple guarantees. Worker must run exact full CI commands `deder exec -t compile` and `deder exec -t test` after repair; parent owns commit/push to existing fix/named-tuple-options branch and checks remote CI. Scope includes affected downstream tests/callers but avoid merely changing tests to hide a production regression.

Review repair authorized: user says `ok fix them` after assessment of three CodeRabbit findings. Fix all confirmed cases in phases/review/review-report.md: union arm selection with optional defaults (both codecs), unbracketed structured Option round trip (both codecs), abstract generic named-tuple validator compilation. Preserve standalone missing-field defaults, concrete named-tuple recursive validation, explicit validator precedence, and previous union fallback. Add permanent behavior/compile regression coverage; no test weakening. Update existing PR with scoped commit and verify remote CI.
