# Targeted parent gate follow-up

Initial report remains immutable. Before acceptance, add permanent regression cases in both codec suites for multi-field aliased named tuples and a three-arm union (later supplied arm must be selected over earlier all-default arms). The new recursive metadata matcher warrants these boundary checks. Existing absence/ambiguity cases remain intact. Only touched test changes needed unless a concrete failure appears; if production repair is required, report the failure first. Avoid incidental formatting changes (extra blank line in LowPriority instance body is unnecessary).

Run affected JVM suite checks after final test changes. Prior broad source checks remain valid if production source is unchanged; no need to repeat broad matrix for added tests. New immutable terminal report fix-review-2-report.md, include checks and pointers. Parent handles commit/push.
