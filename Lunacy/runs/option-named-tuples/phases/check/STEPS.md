# Check phase

| Step | State | Scope | Report |
|---|---|---|---|
| check-option | FINAL | Inspect existing implementations and callers; add focused tests for optional named-tuple fields in Querson, Formson, Validson and relevant parsing behavior; verify using Deder; request decision before production changes or material expansion | phases/check/check-option-report.md |

Worker must inventory/reuse existing system first, then implement tests, verify, self-review, fix tests if needed, terminal reverify. Keep logs in evidence files, not mailbox. Immutable report begins with concise Control Block: result, behavior matrix, changed paths, verification counts/commands, exact parent inspection pointers, residuals, next action. Only BLOCKED, DECISION_REQUIRED, FINAL mailbox messages, at most three short lines. Stop before conflicting scope/ownership changes. Token usage unavailable unless host supplies exact counters.
