## Terminal verification

The final test state is in the detached worktree `/home/sake/projects/sake92/sharaf/.worktrees/option-named-tuples`.

- `deder exec -t test -m querson-jvm-test-3.9.0`: PASS; 4 suites, 24 tests.
  Full output: `/tmp/option-named-tuples-evidence/querson-final.log`.
- `deder exec -t test -m formson-jvm-test-3.9.0`: PASS; 4 suites, 24 tests.
  Full output: `/tmp/option-named-tuples-evidence/formson-terminal.log`.
- `deder exec -t test -m validson-jvm-test-3.9.0`: PASS; 1 suite, 7 tests.
  Full output: `/tmp/option-named-tuples-evidence/validson-terminal-final.log`.
- `git diff --check`: PASS.

The initial Deder attempt without escalation was blocked by sandbox network/Unix-socket restrictions; escalated reruns passed. The expected compile-time probe `Validator.derived[(value: Option[SimpleData])]` fails with `ExprCastException: Expected type: scala.Product; Actual type: value`; output is in `/tmp/option-named-tuples-evidence/validson-derived-failure.log`.
