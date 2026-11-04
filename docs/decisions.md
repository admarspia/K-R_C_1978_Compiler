# Decision Log

Format: decision, reasons, rejected options, revisit trigger.
Constraints are defined in `constraints.md`: C1 finishable solo, C2 fidelity to
original C, C3 testable rules, C4 book programs run, C5 good error messages,
C6 doesn't block later phases.

Status values: Active, Amended, Superseded.
Do not edit a decision's meaning in place. Add a dated note or a new entry.

Last updated: 2026-11-04

## Index

| ID  | Title                                             | Status     |
|-----|---------------------------------------------------|------------|
| D1  | Target language: K&R C (1978)                     | Active     |
| D2  | Version 1 scope: staged                           | Active     |
| D3  | Comments: `/* */` only                            | Active     |
| D4  | Identifier significance: all characters           | Superseded by D10 |
| D5  | Error reporting: collect lexical errors           | Amended    |
| D6  | Machine model: integer and long widths            | Active     |
| D7  | `fortran` and `asm` are not reserved              | Active     |
| D8  | Malformed character and string constants          | Active     |
| D9  | White space                                       | Active     |
| D10 | Identifier significance: first 8 characters       | Active     |

---

## D1. Target language: K&R C (1978)

**Status:** Active

**Decision:** The language is C as described in The C Programming Language,
1st edition (Kernighan & Ritchie, 1978). Appendix A (the C Reference Manual)
is the reference.

**Reasons**
- Best fit for C2 and C4: it is the original, and the book's programs target it.
- A complete reference exists (Appendix A), so the spec has a source of truth.
- Medium complexity: no prototypes, `void`, `const` or `enum` to handle.

**Rejected**
- ANSI C89: a later language, and it adds prototypes and more types.
- K&R plus extensions: the spec becomes harder to keep consistent.
- Pre-1978 C: poorly documented, and the syntax is odd (`=+`).

**Revisit when:** never for the target. Extensions are separate, labeled decisions.

---

## D2. Version 1 scope: staged

**Status:** Active

**Decision:**
- Stage 1: `int`, `char`, expressions, `if`, `while`, `for`, functions.
- Stage 2: pointers, arrays, string literals.
- Later: `struct`, `float`/`double`, `long`/`short`/`unsigned`, `switch`, `do`,
  `goto`, `union`, `typedef`, the preprocessor.
- The lexer recognizes all token classes in Appendix A section 2 from day one,
  including strings, character constants, floating constants and all 28 keywords,
  even before the parser accepts them.

**Reasons**
- C1: a tiny pipeline that works end to end shows design mistakes while they are
  cheap to fix.
- C6: pointers and arrays force a real type representation, so stage 2 is written
  into the spec now to avoid a type design that blocks it.
- The book's hello world needs a string literal, so the lexer must handle it early.
- The lexical rules are one self-contained section of the book, so implementing
  all of them at once is cheaper than returning to the lexer for every stage.

**Rejected**
- Medium or large scope first: slow to a working pipeline, and large type work.
- Minimal only, with no plan for stage 2: risks a type design that can't grow.

**Revisit when:** stage 1 passes all its example programs.

---

## D3. Comments: `/* */` only

**Status:** Active. Confirmed by Appendix A 2.1 on 2026-11-04.

**Decision:** Only `/* ... */` comments exist, and they do not nest. `//` has no
special meaning: it lexes as two `/` operators and normally causes a parse error.
An unterminated `/*` is a lexical error.

**Reasons**
- C2: `//` is not in K&R C, so accepting it would compile a different language.
  A.2.1 defines only `/*` and `*/` and says comments do not nest.
- C3: one comment form gives one simple, testable rule.
- Accepting `//` would change the meaning of some valid K&R code, such as
  `a //* x */ b`.

**Rejected**
- `//` as an extension: lowers fidelity and needs tests for both forms.
- `//` behind a flag: extra code and two test modes that may never be used.

**Revisit when:** you want a comfort extension. It would be its own decision, with
the flag design written first.

---

## D4. Identifier significance: all characters

**Status:** Superseded by D10 on 2026-11-04.

**Original decision:** All characters of an identifier are significant. The K&R
limit was documented but not enforced.

**Why superseded:** Appendix A 2.2 says no more than the first eight characters are
significant. Treating all characters as significant changes the language, which
breaks C2. See D10.

---

## D5. Error reporting: collect lexical errors, stop before parsing

**Status:** Amended on 2026-11-04 (recovery table extended after reading
Appendix A section 2 and adding D6 and D8).

**Decision:** The lexer records each error with line, column and message, and keeps
scanning. If any lexical error was recorded, the error list is reported and the
parser does not run.

Recovery rules:

| Error                                            | Recovery                          |
|--------------------------------------------------|-----------------------------------|
| Unrecognized character                           | skip that character               |
| Unterminated comment                             | reaches end of file, stop         |
| Unterminated string or character constant (D8)   | skip to end of line               |
| Empty or multi-character constant (D8)           | consume through the closing quote |
| Octal escape value too large (D8)                | keep scanning the constant        |
| Malformed floating constant (`1e`, `1e+`)        | consume the whole bad literal     |
| Hex constant with no digits (`0x`)               | consume the whole bad literal     |
| Integer constant too large (D6)                  | consume the whole literal         |

Not errors in K&R C:
- An unknown escape: `'\q'` is `q`, and the backslash is ignored (A.2.4.3).
- The digits 8 and 9 in an octal constant (A.2.4.1).
- A floating constant such as `25.`, `.5` or `1e5` (A.2.4.4).

**Reasons**
- C5: the user sees all lexical errors in one run.
- C6: the parser only ever receives clean tokens, and the error-list structure
  carries over to later phases.
- C3: each recovery rule is a small, testable behavior.
- Starting with fail-fast would mean restructuring the lexer later.

**Rejected**
- Fail fast: simplest, but one error per run and a later rewrite.
- Collect everywhere with parser recovery: best experience, but the parser needs
  synchronization rules that don't exist yet.

**Revisit when:** the parser exists. Then decide whether to add parser-level
recovery (synchronizing on `;` and `}`) and a shared error list across phases.

---

## D6. Machine model: integer and long widths

**Status:** Active

**Decision:** The target model is:

| Type   | Width   |
|--------|---------|
| char   | 8 bits  |
| short  | 16 bits |
| int    | 32 bits |
| long   | 32 bits |
| float  | 32 bits |
| double | 64 bits |

Verify against the table in A.2.6. As recalled, these match the IBM 370 and
Interdata 8/32 columns.

Constant rules under this model:
- A decimal constant above 2147483647 is an error: "integer constant too large".
- An octal or hex constant above 0xFFFFFFFF is an error: "integer constant too large".
- A trailing `l` or `L` gives the constant type long. It has the same width as int,
  so the value range is unchanged.
- The lexer stores the numeric value in a 64-bit Java integer, so it can check the
  range before reporting an error.

**Reasons**
- C2: A.2.4.2 explicitly allows int and long to be identical on some machines, so
  this model is within the language.
- C1 and C6: one integer width for int and long keeps the type checker, conversions
  and code generation simpler in the early stages.
- 32-bit int is a natural fit for the modern machines you are likely to target.

**Rejected**
- 16-bit int, 32-bit long (PDP-11): most "original", but 16-bit arithmetic is awkward
  to generate on a modern CPU, and many book programs assume more range.
- 32-bit int, 64-bit long (modern LP64): further from the book's machines, and it
  brings a width the book never describes.
- 36-bit (Honeywell 6000): impractical on modern hardware.

**Revisit when:** the code-generation target is chosen. If long becomes 64 bits, the
"too large" thresholds for decimal constants change, and a decimal constant above
2147483647 becomes a valid long.

---

## D7. `fortran` and `asm` are not reserved

**Status:** Active

**Decision:** The reserved words are exactly the 28 listed in A.2.3. `fortran` and
`asm` are ordinary identifiers.

The 28 words: `int char float double struct union long short unsigned auto extern
register typedef static goto return sizeof break continue if else for do while
switch case default entry`. `entry` is reserved but unused.

**Reasons**
- C2: the book says only that "some implementations" reserve `fortran` and `asm`,
  so they are not part of the language.
- C3: a fixed, complete keyword list is simple to test.

**Rejected**
- Reserve them: it would stop valid K&R programs from using those names, for
  features we don't implement.

**Revisit when:** you add inline assembly or a foreign-call feature.

---

## D8. Malformed character and string constants

**Status:** Active

**Decision:** A.2 doesn't say what an empty, multi-character or unterminated
constant means, so these rules fill the gap.

| Case                                       | Result                                       |
|--------------------------------------------|----------------------------------------------|
| `''` (empty character constant)            | error: empty character constant              |
| `'ab'` (more than one character)           | error: multi-character constant              |
| Raw newline inside `'...` or `"..."`       | error: unterminated constant or string       |
| End of file inside `'...` or `"..."`       | error: unterminated constant or string       |
| `\` followed by newline inside a string    | ignored, the string continues (A.2.5)        |
| `\` followed by an unlisted character      | backslash ignored (A.2.4.3)                  |
| `\ddd` with a value above 255              | error: octal escape out of range             |
| `\8` or `\9`                               | the backslash is ignored, giving `8` or `9`  |

Recovery follows the D5 table.

**Reasons**
- A.2.4.3 says a character constant is "a character", which excludes zero or several.
- A raw newline is only legal in a string when escaped (A.2.5), so a bare one is an
  unterminated string.
- C5: each case has its own message, which makes the line and column useful.
- Range-checking `\ddd` against the 8-bit char from D6 catches typos like `'\777'`.

**Rejected**
- Treat multi-character constants as an integer, as some compilers do: not in K&R.
- Silently truncate an oversized octal escape: hides bugs.
- Make unknown escapes an error: contradicts A.2.4.3.

**Revisit when:** a book program uses a construct these rules reject.

---

## D9. White space

**Status:** Active

**Decision:** White space is the blank, the tab, the newline and comments (A.2).
In addition, the carriage return `\r` is accepted as a blank (extension). `\f` and
`\v` are not accepted, so they are "unrecognized character".

**Reasons**
- C2: the book lists only blanks, tabs and newlines.
- Practicality: source files with Windows line endings (`\r\n`) would fail at every
  line otherwise. `\r` handling doesn't change program meaning.

**Rejected**
- Strict reading (reject `\r`): breaks files from many editors, for no benefit.
- Also accept `\f` and `\v`: not needed yet, and each extension must be recorded.

**Revisit when:** a book example contains a form feed.

---

## D10. Identifier significance: first 8 characters

**Status:** Active. Supersedes D4.

**Decision:** The first 8 characters of an identifier are significant, as in A.2.2.
The limit is defined as one named constant in one place.

- The lexer keeps the full spelling in the token. It does not truncate.
- The symbol table builds its lookup key from the first 8 characters.
- When two different spellings share the same first 8 characters, the compiler emits
  a warning naming both spellings. In the same scope, the second declaration is the
  usual "redeclaration" error.
- The external-name limits in A.2.2 (6 to 8 characters, with case rules, for various
  assemblers and loaders) are not enforced: they describe old linkers.

**Reasons**
- C2: it follows the book.
- C6: keeping the spelling in the token gives good messages, and only the symbol table
  needs to know about the limit.
- The warning makes a rule that is silent in the original compiler easier to live with.
- Because the limit is one constant, changing or lifting it later is a one-line change.

**Rejected**
- All characters significant (the old D4): changes program meaning and breaks C2.
- Silent truncation with no warning: it works, but collisions like `counter_one` and
  `counter_two` give confusing errors.
- Truncating in the lexer: loses the spelling for error messages.
- Enforce the external-name limits: tied to hardware that no longer exists.

**Revisit when:** a book program relies on truncated names being equal, or you add
support for linking against other C code.
