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


## D6 amendment (2026-11-04): constant typing

Under the D6 model (int and long both 32 bits), the rules from A.2.4.1 become:

| Constant form          | Value range                    | Type and result                      |
|------------------------|--------------------------------|--------------------------------------|
| Decimal                | 0 to 2147483647                | int                                  |
| Decimal                | above 2147483647               | error: integer constant too large    |
| Octal or hex           | 0 to 0xFFFFFFFF                | int; values above 0x7FFFFFFF wrap to negative (two's complement) |
| Octal or hex           | above 0xFFFFFFFF               | error: integer constant too large    |
| Any, with `l` or `L`   | same limits as above           | long                                 |

Reason: A.2.4.1 makes an octal or hex constant long only when it exceeds the largest
unsigned integer, so values between the largest signed and the largest unsigned int
are still int.

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

## D11. Plain `char` signedness  (CONFIRMED 2026-11-04)

**Status:** Active. Confirmed by A.6.1.

**Decision:** Plain `char` is signed, ranging from -128 to 127. A character
constant with an octal escape is sign-extended: `'\377'` is -1.

**Reasons**
- A.6.1: char signedness is machine-dependent; the PDP-11 sign-extends, and the
  book's own example gives `'\377'` the value -1.
- The standard character set is always non-negative, so ordinary text is unaffected.

**Revisit when:** a book program assumes unsigned char behavior.


---

## D12. Distinct types, even when widths are equal

**Status:** Active

**Decision:** short, int and long are three distinct types in the compiler's type
system, as are float and double, even where D6 gives two of them the same width
(int and long are both 32 bits).

**Reasons**
- A.4 allows an implementation to make them equivalent, but doesn't require it.
- C6: with distinct types, moving long to 64 bits later changes one table entry, and
  not the type checker or the symbol table.
- Pointer and function types that mention long stay different from those that
  mention int, as the language's type rules expect.

**Rejected**
- Treat long as an alias of int under D6: simpler today, but it makes the 64-bit
  switch a rewrite and hides type errors.

**Revisit when:** never, unless the type system is redesigned.

---

## D13. `register` is a hint

**Status:** Active

**Decision:** `register` is accepted wherever the book allows it. In stage 1 and 2
a register variable is allocated like an automatic one. Rules the book attaches to
register variables are checked by the semantic phase (verify them in sections 7
and 8).

**Reasons**
- A.4: storage in a register is "if possible", so it is a hint and not a guarantee.
- C1: register allocation is an optimization, and belongs to a later stage.

**Rejected**
- Real register allocation in v1: large work for no change in program meaning.
- Reject `register`: breaks book programs that use it.

**Revisit when:** you add an optimizer or a register allocator.

---

## D14. Grammar notation

**Status:** Active

**Decision:** The grammar is written in plain-text EBNF using the mapping in
`grammar.md` ("Grammar Notation"). The `{ ... }` repetition form is an extension,
allowed only as a mechanical rewrite of a recursive book rule, and marked with the
rule it replaces.

**Reasons**
- C3: one notation, written once, keeps every rule consistent and testable.
- The book's italic and bold have no plain-text form, and the mapping is a
  one-to-one translation.

**Rejected**
- Reproduce the book's typography with HTML or LaTeX: harder to write and diff.
- Use `{ }` freely: it would hide differences between your grammar and the book's.

**Revisit when:** the parser design needs a different shape, such as removing left
recursion. Record that as a separate, labeled transformation.

---

## D15. `unsigned long` is not supported

**Status:** Active

**Decision:** `unsigned long` is rejected by the semantic phase (declaration error:
"unsigned long is not supported"). `unsigned` means `unsigned int`.

**Reasons**
- A.4 says the PDP-11 does not support unsigned long.
- A.6.6 has no rule for combining unsigned long with other types: read literally, any
  operand that is long forces the result to plain long, which would silently change
  the signedness.
- C1: it removes a corner of the conversion rules for no loss of the book's programs.

**Rejected**
- Support it with my own extra conversion rule: an invention the book does not make.
- Support it using the literal A.6.6 rule: gives surprising results.

**Revisit when:** a book program needs unsigned long, or after the book's section 8
declarations are read, if they treat it differently.

---

## D16. Float to integer conversion

**Status:** Active

**Decision:** Converting a floating value to an integral type truncates toward zero.
If the value does not fit the destination, the result is undefined, as in A.6.3.

**Reasons**
- A.6.3 says the direction of truncation is machine-dependent, so the compiler must
  pick one.
- Truncation toward zero is what nearly every instruction set and language does,
  so it is the least surprising choice.
- C3: one fixed rule is testable.

**Rejected**
- Round to nearest: not what the book's machines are described as doing.
- Define out-of-range results: no benefit, and the book leaves them undefined.

**Revisit when:** the code-generation target is chosen.

## D17. Expression grammar is layered by precedence

**Status:** Active

**Decision:** The expression grammar has one rule per precedence level (15 levels,
see `grammar.md`), instead of copying the book's ambiguous rules. Using `{ }` for
left-associative levels is part of this transformation and extends D14.

**Reasons**
- A.7 says precedence is the order of the subsections, so the book's grammar needs
  that outside information to be unambiguous.
- C6: a layered grammar maps directly to either recursive descent or precedence
  climbing, and the parser needs no extra tables.
- C3: each level is a separate rule with its own tests.

**Rejected**
- Copy the book's rules and resolve ambiguity in the parser with a table: works, but
  the grammar document would no longer be a complete definition of the language.

**Revisit when:** the parser design is chosen. A precedence-climbing parser may read
the table in `grammar.md` instead; the grammar stays the specification either way.

---

## D18. Compound assignment operators are single tokens

**Status:** Active. Restriction of the book (verify wording of A.7.14).

**Decision:** `+=`, `-=`, `*=`, `/=`, `%=`, `>>=`, `<<=`, `&=`, `^=` and `|=` are each
one token. The spaced form `x + = 1` is rejected, because `+` followed by `=` is not
a valid expression.

**Reasons**
- A.7.14 says the two parts "are separate tokens", which, read literally, allows
  blanks between them.
- C1 and C6: one token per operator means the parser and AST carry a single
  operator each, and no adjacency check is needed.
- A.2's longest-match rule already produces single tokens for `==`, `<=`, `>>` and
  the rest, so this is consistent.
- C2 costs almost nothing: spaced compound operators are essentially never written.

**Rejected**
- Lex as two tokens and let the parser pair them: faithful, but it pushes a
  lexical decision into the grammar, and adds a rule about adjacency.
- Let the lexer merge `+` and `=` across white space: needs unbounded lookahead
  through blanks and comments, beyond what a pushback reader gives.

**Revisit when:** a book program uses a spaced compound operator.

---

## D19. Evaluation order

**Status:** Active

**Decision:** The book leaves the order undefined, so the compiler picks one:
- Operands of binary operators and arguments of calls are evaluated left to right.
- `&&`, `||`, `?:` and `,` follow A.7.11 to A.7.15 exactly.
- The compiler does not rearrange `*` `+` `&` `|` `^` in stages 1 and 2.
- How arguments are passed is the code generator's concern; evaluating left to right
  into temporaries is compatible with pushing them in reverse.

**Reasons**
- A.7 allows any order, so a fixed order is always correct.
- C3: deterministic output makes expression tests exact.
- A later optimizer may use the freedom the book grants (A.7: rearrangement).

**Rejected**
- Leave unspecified: untestable, and varies with the implementation.
- Right to left for arguments: matches some old compilers, but makes behavior depend
  on a stack convention.

**Revisit when:** an optimizer is added.

---

## D20. Machine-dependent arithmetic

**Status:** Active

**Decision:** Choices where A.6 and A.7 say "machine-dependent":

| Area                          | Choice                                                         |
|-------------------------------|----------------------------------------------------------------|
| Integer overflow              | wraps in two's complement, no check (A.7: existing C ignores it) |
| Division and `%` by zero      | in a constant expression: compile error; at run time: not checked, host behavior |
| Integer division, negatives   | truncates toward zero; remainder has the sign of the dividend  |
| Right shift                   | arithmetic for signed left operands, logical for unsigned (A.7.5) |
| Shift count negative or >= width | undefined; a constant count out of range gives a warning   |
| Floating-point exceptions     | not handled; host behavior                                     |

**Reasons**
- Every row matches what the book says its machines do, or what A.7 allows.
- C3: each is a testable rule, and none needs run-time checks.
- Consistent with D16 (float to integer truncates toward zero).

**Rejected**
- Run-time checks for overflow or divide by zero: not in the book, and a code
  generation cost with no language benefit.
- Logical right shift for signed values: allowed ("may be"), but A.7.5 says
  arithmetic is what the PDP-11 does.

**Revisit when:** the code-generation target is chosen.

---

## D21. Pointer and integer mixing

**Status:** Active

**Decision:** The following are accepted, and each produces a warning ("nonportable
conversion"), except where the integer is the constant 0:
- assigning an integer to a pointer, or a pointer to an integer,
- assigning a pointer to a pointer of another type (pure copy),
- comparing a pointer with an integer.

Assigning or comparing the constant 0 with a pointer is silent, and 0 is the null
pointer (A.7.14, A.7.7). Arithmetic combinations A.7.4 does not allow are errors.

**Reasons**
- A.7.14 says compilers allow these copies, and book programs may depend on it (C4).
- C5: a warning flags the nonportable use without breaking fidelity.

**Rejected**
- Reject all: breaks fidelity and some book programs.
- Accept silently: hides errors the book itself calls nonportable.

**Revisit when:** a book program produces a warning that is clearly a false alarm.
