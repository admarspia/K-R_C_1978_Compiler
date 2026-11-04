# Decision Log

Format: decision, reasons, rejected options, revisit trigger.
Constraints used: C1 finishable solo, C2 fidelity to original C, C3 testable rules,
C4 book programs run, C5 good error messages, C6 doesn't block later phases.

Date: 2026-11-04

---

## D1. Target language: K&R C (1978)

**Decision:** The language is C as described in The C Programming Language,
1st edition (Kernighan & Ritchie, 1978). Appendix A is the reference.

**Reasons**
- Best fit for C2 and C4: it is the original, and the book's programs target it.
- A complete reference exists (Appendix A), so the spec has a source of truth.
- Medium complexity: no prototypes, `void`, `const` or `enum` to handle.

**Rejected**
- ANSI C89: a later language, and it adds prototypes and more types.
- K&R plus extensions: the spec becomes harder to keep consistent.
- Pre-1978 C: poorly documented, and the syntax is odd (`=+`).

**Revisit when:** never for the target. Extensions would be a separate,
labeled decision.

---

## D2. Version 1 scope: staged

**Decision:**
- Stage 1: `int`, `char`, expressions, `if`, `while`, `for`, functions.
- Stage 2: pointers, arrays, string literals.
- Later: `struct`, `float`/`double`, `long`/`short`/`unsigned`, `switch`, `do`,
  `goto`, `union`, `typedef`, the preprocessor.
- The lexer recognizes string and character literals from day one, even before
  the parser accepts them.

**Reasons**
- C1: a tiny pipeline that works end to end shows design mistakes while they
  are cheap to fix.
- C6: pointers and arrays force a real type representation, so stage 2 is
  written into the spec now to avoid a type design that blocks it.
- The book's hello world needs a string literal, so the lexer must handle it early.

**Rejected**
- Medium or large scope first: slow to a working pipeline, and large type work.
- Minimal only, with no plan for stage 2: risks a type design that can't grow.

**Revisit when:** stage 1 passes all its example programs.

---

## D3. Comments: `/* */` only

**Decision:** Only `/* ... */` comments exist, and they do not nest.
`//` has no special meaning: it lexes as two `/` operators and normally
causes a parse error. An unterminated `/*` is a lexical error.

**Reasons**
- C2: `//` is not in K&R C, so accepting it would compile a different language.
- C3: one comment form gives one simple, testable rule.
- Accepting `//` would change the meaning of some valid K&R code, such as
  `a //* x */ b`.

**Rejected**
- `//` as an extension: lowers fidelity and needs tests for both forms.
- `//` behind a flag: extra code and two test modes that may never be used.

**Revisit when:** you want a comfort extension. It would be its own decision,
with the flag design written first.

---

## D4. Identifier significance: full spelling, 8-character rule documented only

**Decision:** All characters of an identifier are significant.
The K&R limit (first 8 characters for internal names, fewer for external names;
verify exact numbers in the book) is documented but not enforced.

**Reasons**
- C6: the symbol table keys on the full spelling, with no truncation logic.
- Programs that don't rely on truncation behave the same either way.
- The external-name limit concerns the linker, which doesn't apply if the
  compiler's output is handed to a modern linker.

**Rejected**
- Silent truncation to 8: `counter_one` and `counter_two` collide with no message.
- Truncate plus error on collision: stricter, but extra code for little benefit in v1.

**Revisit when:** a book program depends on truncation, or when you add warnings.
The most likely upgrade is a warning when two names differ only after position 8.

---

## D5. Error reporting: collect lexical errors, stop before parsing

**Decision:** The lexer records each error with line, column and message, and
keeps scanning. If any lexical error was recorded, the error list is reported
and the parser does not run.

Recovery rules:
- unrecognized character: skip that character,
- unterminated string or character constant: skip to the end of the line,
- malformed number: consume the whole bad literal,
- unterminated comment: reaches end of file, so scanning stops.

**Reasons**
- C5: the user sees all lexical errors in one run.
- C6: the parser only ever receives clean tokens, and the error-list structure
  carries over to later phases.
- C3: each recovery rule is a small, testable behavior.
- Starting with fail-fast would mean restructuring the lexer later.

**Rejected**
- Fail fast: simplest, but one error per run and a later rewrite.
- Collect everywhere with parser recovery: best experience, but the parser
  needs synchronization rules that don't exist yet.

**Revisit when:** the parser exists. Then decide whether to add parser-level
recovery (synchronizing on `;` and `}`) and a shared error list across phases.
