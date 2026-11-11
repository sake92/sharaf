## Control Block

- Result: PASS; repaired Querson, Formson, and Validson named-tuple Option behavior in `/home/sake/projects/sake92/sharaf/.worktrees/option-named-tuples`.
- Behavior: absent `Option` named-tuple fields now use codec defaults; structured and nested `Option` values parse/write/round-trip; invalid values retain field paths; named-tuple validation recurses through `Some` and accepts `None`.
- Production: `QueryStringRW`/`FormDataRW` Option parsing and named-tuple defaults; Querson/Formson indexed object/sequence merging; Validson named-tuple implicit derivation and Product macro cast.
- Tests: characterization expectations replaced with positive regression coverage for present, absent, empty, invalid, nested, and implicit/explicit derivation cases.
- Red evidence: `/tmp/option-named-tuples-evidence/querson-repair-red.log`, `formson-repair-red.log`, `validson-repair-red.log` (each reproduced its assigned defect).
- Terminal JVM: Querson 4 suites/24 tests PASS; Formson 4 suites/24 tests PASS; Validson 1 suite/7 tests PASS.
- Terminal JS: Querson 4 suites/24 tests PASS; Validson 1 suite/7 tests PASS. Formson JS is disabled by project configuration.
- Terminal Native: Querson 4 suites/24 tests PASS; Formson 4 suites/24 tests PASS; Validson 1 suite/7 tests PASS.
- Logs: `/tmp/option-named-tuples-evidence/{querson,formson,validson}{,-js,-native}-terminal-final.log` (JVM Validson is `validson-terminal-final.log`).
- `git diff --check`: PASS. No commits or pushes. Unrelated root files preserved.

## Parent inspection

- `querson/src/ba/sake/querson/QueryStringRW.scala:176-200` and `:496-509`; `querson/src/ba/sake/querson/parse.scala:70-81`.
- `formson/src/ba/sake/formson/FormDataRW.scala:208-232` and `:587-597`; `formson/src/ba/sake/formson/parse.scala:68-77`.
- `validson/src/ba/sake/validson/Validator.scala:122-133` and `:146-173`.
- Regression tests: Querson `QueryStringParseSuite.scala:240-284`, Formson `FormDataParseSuite.scala:260-313`, Validson `ValidsonSuite.scala:101-115`.
- Bounded acceptance command: `git -C /home/sake/projects/sake92/sharaf/.worktrees/option-named-tuples diff --check` (PASS); inspect the listed production/test regions and run one targeted JVM module check if desired.
