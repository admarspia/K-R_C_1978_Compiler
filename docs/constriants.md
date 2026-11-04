# Constraints

The constraints that decisions in `decisions.md` are judged against.
When a decision is made, each option is scored against these. If a constraint
changes, revisit the decisions that cite it.

Date: 2026-11-04

## Summary

| ID | Name                         | Priority |
|----|------------------------------|----------|
| C1 | Finishable solo              | High     |
| C2 | Fidelity to original C       | High     |
| C3 | Testable rules               | High     |
| C4 | Book programs run            | Medium   |
| C5 | Good error messages          | Medium   |
| C6 | Doesn't block later phases   | High     |

---

## C1. Finishable solo

**Statement:** The project must be completable by one person, in stages, with a
working pipeline at the end of each stage.

**Rationale:** A large scope that never runs end to end teaches less than a small
one that does. Early working stages expose design mistakes while they are cheap to fix.

**Check:** Each stage can be described as "these example programs compile and run."

**Cited by:** D2, D5

---

## C2. Fidelity to original C

**Statement:** The language implemented is K&R C (1978), as described in
Appendix A of the first edition. Behavior that differs from it is an
explicit, recorded extension.

**Rationale:** The goal is to build the original language, not a nearby dialect.

**Check:** Every language feature in the spec can be traced to a section of
Appendix A, or is listed as an extension in `decisions.md`.

**Cited by:** D1, D3, D4

---

## C3. Testable rules

**Statement:** Every rule in the spec must be checkable by an example: one valid
case, and one invalid case with the expected error.

**Rationale:** Rules that cannot be tested drift between the spec, the code and the
tests, and become bugs.

**Check:** Each rule has at least one test in `tests/` that would fail if the rule broke.

**Cited by:** D3, D5

---

## C4. Book programs run

**Statement:** The example programs from the first edition should compile and run,
within the supported stage.

**Rationale:** They are the closest thing to an official test suite for the language,
and they show the language is useful in the way the authors intended.

**Check:** The programs in `examples/` lex, parse and run correctly for the stage
they belong to.

**Cited by:** D1, D2

---

## C5. Good error messages

**Statement:** Every reported error includes the line, the column, and a message
that says what was wrong.

**Rationale:** Compiler errors are the user interface. Vague messages make the
compiler hard to use and hard to debug.

**Check:** Each error case in the spec specifies its exact message and position,
and the tests assert on them.

**Cited by:** D5

---

## C6. Doesn't block later phases

**Statement:** A decision in one phase must not make a later phase (parser, symbol
table, semantic analysis, IR, code generation) hard or impossible to build.

**Rationale:** Late rewrites cost the most. Considering later phases early, for
example the type representation needed by pointers, avoids them.

**Check:** Before accepting a decision, state what it requires of each later phase.
If the answer is "a redesign," reconsider it.

**Cited by:** D2, D4, D5

---

## Adding or changing a constraint

1. Give it the next ID (C7, C8, ...). Do not reuse or renumber old IDs.
2. Fill in all fields: statement, rationale, check, cited by.
3. If an old constraint no longer applies, mark it **Retired** with a date and a reason,
   and keep it in the file.
4. After any change, check the "Cited by" decisions and note in `decisions.md`
   whether they need revisiting.
