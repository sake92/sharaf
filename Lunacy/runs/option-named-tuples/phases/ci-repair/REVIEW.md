# Parent rejection of first repair

The first ci-repair report is immutable but its proposed fix is rejected: deleting the existing validated-union compile calls weakens existing coverage and changes the supported caller surface to hide a regression. Do not commit that diff. Restore the three original playground calls exactly.

Continue as ci-repair-2: fix production implicit selection/derivation so concrete named tuples validate recursively while existing union callers still compile. Preserve previous union fallback behavior if soundly implementing union validation is outside this repair; do not promise runtime branch validation for unions. A small macro guard that detects a concrete named tuple vs union may be appropriate; prefer clean local solution over a catalog of evidence hacks. Add focused compile regression coverage for existing union callers and retain concrete Option validation tests. Explicit user validators must retain precedence.

Own full compile and test verification again after final production edit. Immutable new report phases/ci-repair/ci-repair-2-report.md. Report any unavoidable contract ambiguity before weakening callers/tests.
