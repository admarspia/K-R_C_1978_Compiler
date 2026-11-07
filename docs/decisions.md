# Decision Log

Format: status, decision, reasons, rejected options, revisit trigger.
Constraints are defined in `constraints.md`: C1 finishable solo, C2 fidelity to
original C, C3 testable rules, C4 book programs run, C5 good error messages,
C6 doesn't block later phases, C7 docs are authoritative.

Source references use "A.n" for section n of Appendix A (C Reference Manual) of
The C Programming Language, 1st edition, 1978.

Status values: Active, Amended, Superseded.
Do not edit a decision's meaning in place. Add a dated amendment or a new entry.

Last updated: 2026-11-06

## Index

| ID  | Title                                                    | Status            |
|-----|----------------------------------------------------------|-------------------|
| D1  | Target language: K&R C (1978)                            | Active            |
| D2  | Staged scope                                             | Amended           |
| D3  | Comments: `/* */` only                                   | Active            |
| D4  | Identifier significance: all characters                  | Superseded by D10 |
| D5  | Error reporting: collect lexical errors                  | Amended           |
| D6  | Machine model                                            | Amended           |
| D7  | `fortran` and `asm` are not reserved                     | Active            |
| D8  | Character constants and strings                          | Amended           |
| D9  | White space and source character set                     | Amended           |
| D10 | Identifier significance: first 8 characters              | Active            |
| D11 | Plain `char` is signed                                   | Amended           |
| D12 | Distinct types, even when widths are equal               | Active            |
| D13 | `register` is a hint                                     | Amended           |
| D14 | Grammar notation                                         | Active            |
| D15 | Type specifier combinations; no `unsigned long`          | Amended           |
| D16 | Float to integer conversion truncates toward zero        | Active            |
| D17 | Expression grammar is layered by precedence              | Active            |
| D18 | Compound assignment operators are single tokens          | Active            |
| D19 | Evaluation order                                         | Active            |
| D20 | Machine-dependent arithmetic                             | Active            |
| D21 | Pointer and integer mixing                               | Active            |
| D22 | Source priority: detailed sections over the summary      | Active            |
| D23 | Name classes                                             | Active            |
| D24 | Member names are one global class                        | Active            |
| D25 | Implicit declarations                                    | Active            |
| D26 | Struct and union operations are strict                   | Active            |
| D27 | Preprocessor is a separate pass, stage 6                 | Active            |
| D28 | Constant expressions                                     | Active            |
| D29 | Old-style function definitions only                      | Active            |
| D30 | Bit-fields are deferred                                  | Active            |
| D31 | Anachronisms are not supported                           | Active            |
| D32 | Interpretations of ambiguous wording                     | Active            |

---

## D1. Target language: K&R C (1978)

**Status:** Active

**Decision:** The language is C as described in The C Programming Language,
1st edition (Kernighan & Ritchie, 1978). Appendix A (the C Reference Manual) is the
reference.

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

## D2. Staged scope

**Status:** Amended 2026-11-06

**Decision:** The language is built in six stages (full table in `spec.md`):

| Stage | Adds                                                                         |
|-------|------------------------------------------------------------------------------|
| 1     | `int`, `char`, all operators on them, expressions, `if`, `while`, `for`, `return`, `break`, `continue`, null and compound statements, functions (old style), storage classes `auto` `static` `extern` `register`, scalar initializers, implicit declarations |
| 2     | pointers, arrays, string literals, aggregate initializers, subscripting, `*` and `&` |
| 3     | `struct`, `.` and `->`                                                       |
| 4     | `short`, `long`, `unsigned`, `float`, `double`                               |
| 5     | `switch`, `do`, `goto` and labels, `union`, `typedef`, bit-fields            |
| 6     | the preprocessor                                                             |

- The lexer is complete from day one: it recognizes every token class in A.2,
  including strings, character, floating and long constants and all 28 keywords,
  even before the parser accepts them.
- A construct that belongs to a later stage produces the error
  "not supported yet (stage N)" from the earliest phase that recognizes it.

**Amendment (2026-11-06):** stage 1 now includes `return`, `break`, `continue`, the
null statement, the four non-typedef storage classes, old-style function definitions
and implicit declarations, because small book programs need them.

**Reasons**
- C1: a tiny pipeline that works end to end shows design mistakes while they are
  cheap to fix.
- C6: pointers and arrays force a recursive type representation, so stage 2 is
  planned now to avoid a type design that blocks it.
- A.2 is one self-contained section, so implementing all of it at once is cheaper
  than returning to the lexer at every stage.

**Rejected**
- Medium or large scope first: slow to a working pipeline, and large type work.
- Minimal only, with no plan for stage 2: risks a type design that can't grow.

**Revisit when:** stage 1 passes all its example programs.

---

## D3. Comments: `/* */` only

**Status:** Active. Confirmed by A.2.1.

**Decision:** Only `/* ... */` comments exist, and they do not nest. `//` has no
special meaning: it lexes as two `/` operators and normally causes a parse error.
An unterminated `/*` is a lexical error (L03).

**Reasons**
- C2: `//` is not in K&R C, so accepting it would compile a different language.
  A.2.1 defines only `/*` and `*/` and says comments do not nest.
- C3: one comment form gives one simple, testable rule.
- Accepting `//` would change the meaning of some valid K&R code, such as
  `a //* x */ b`.

**Rejected**
- `//` as an extension: lowers fidelity and needs tests for both forms.
- `//` behind a flag: extra code and two test modes that may never be used.

**Revisit when:** you want a comfort extension. It would be its own decision.

---

## D4. Identifier significance: all characters

**Status:** Superseded by D10 on 2026-11-06.

**Original decision:** All characters of an identifier are significant.

**Why superseded:** A.2.2 says no more than the first eight characters are
significant. Treating all characters as significant changes the language, which
breaks C2. See D10.

---

## D5. Error reporting: collect lexical errors, stop before parsing

**Status:** Amended 2026-11-06

**Decision:** The lexer records each error with line, column and message, and keeps
scanning. If any lexical error was recorded, the error list is reported and the
parser does not run. Warnings are reported but never stop compilation.

Each lexical error has a code, a message and a recovery rule. The catalogue is in
`lexical.md` (section 10). In summary:

| Code | Error                                            | Recovery                          |
|------|--------------------------------------------------|-----------------------------------|
| L01  | Invalid character                                | skip that character               |
| L02  | Invalid character inside a string or character constant | skip that character        |
| L03  | Unterminated comment                             | reaches end of file, stop         |
| L04  | Unterminated string                              | skip to end of line               |
| L05  | Unterminated character constant                  | skip to end of line               |
| L06  | Empty character constant                         | consume through the closing quote |
| L07  | Character constant too long (more than 4)        | consume through the closing quote |
| L08  | Octal escape out of range (above 255)            | keep scanning the constant        |
| L09  | Malformed floating constant (`1e`, `1e+`)        | consume the whole bad literal     |
| L10  | Hex constant with no digits (`0x`)               | consume the whole bad literal     |
| L11  | Integer constant too large                       | consume the whole literal         |
| L12  | Invalid character after a number (`12ab`)        | consume the whole run             |
| L13  | Floating constant out of range                   | consume the whole literal         |

Not errors in K&R C: an unknown escape (`'\q'` is `q`), the digits 8 and 9 in an
octal constant, a float such as `25.`, `.5` or `1e5`, and a multi-character constant
of up to four characters (a warning, W01).

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

**Revisit when:** the parser exists. Then decide whether to add parser-level recovery
(synchronizing on `;` and `}`) and a shared error list across phases.

---

## D6. Machine model

**Status:** Amended 2026-11-06. Verified against the table in A.2.6.

**Decision:** The model matches the IBM 370 and Interdata 8/32 columns of A.2.6:

| Property                     | Value                                              |
|------------------------------|----------------------------------------------------|
| char                         | 8 bits, signed (D11), ASCII                        |
| short                        | 16 bits                                            |
| int                          | 32 bits                                            |
| long                         | 32 bits                                            |
| float                        | 32 bits, IEEE 754 single                           |
| double                       | 64 bits, IEEE 754 double                           |
| pointer                      | 32 bits, byte addresses; `char *` has the same form |
| `sizeof(char)`               | 1                                                  |
| alignment                    | equal to size: char 1, short 2, int, long, float and pointers 4, double 8; aggregates use the strictest alignment of their members (A.14.4) |
| arithmetic                   | two's complement, wraps on overflow (D20)          |
| floating-point arithmetic    | always in double (A.6.2)                           |
| argument evaluation          | left to right (D19)                                |
| bit-field order              | left to right (D30)                                |

Constant rules under this model (A.2.4.1, A.2.4.2):

| Constant form          | Value range                  | Result                                 |
|------------------------|------------------------------|----------------------------------------|
| Decimal                | 0 to 2147483647              | int                                    |
| Decimal                | above 2147483647             | error L11                              |
| Octal or hex           | 0 to 0xFFFFFFFF              | int; values above 0x7FFFFFFF wrap to negative |
| Octal or hex           | above 0xFFFFFFFF             | error L11                              |
| Any, with `l` or `L`   | same limits as above         | long                                   |
| Floating               | within double range          | double                                 |
| Floating               | beyond double range          | error L13                              |

A.2.4.1 makes an octal or hex constant long only when it exceeds the largest
unsigned int, so values between the largest signed and unsigned int stay int.

**Reasons**
- C2: A.2.4.2 and A.4 explicitly allow int and long to be identical, and the
  IBM 370 and Interdata 8/32 columns of A.2.6 are exactly this model.
- C1 and C6: one integer width for int and long keeps the type checker,
  conversions and code generation simpler in the early stages.
- IEEE formats are used because they are what the host provides. The book's own
  hardware formats differ (see the range row of A.2.6), and the book calls the
  differences "less of a problem than might be thought".

**Rejected**
- 16-bit int, 32-bit long (PDP-11): most "original", but 16-bit arithmetic is awkward
  to generate on a modern CPU.
- 32-bit int, 64-bit long (modern LP64): further from the book's machines.
- 36-bit (Honeywell 6000): impractical on modern hardware.
- The book's floating-point formats: no benefit, and no hardware support.

**Revisit when:** the code-generation target is chosen. If long becomes 64 bits, the
"too large" thresholds for decimal constants change, and a decimal constant above
2147483647 becomes a valid long.

---

## D7. `fortran` and `asm` are not reserved

**Status:** Active

**Decision:** The reserved words are exactly the 28 listed in A.2.3:
`int char float double struct union long short unsigned auto extern register typedef
static goto return sizeof break continue if else for do while switch case default
entry`. `entry` is reserved but unused. `fortran` and `asm` are ordinary identifiers.
Keyword recognition uses the full spelling of the word: an identifier such as
`registers` is never a keyword, even though its first eight characters equal one
(see D10).

**Reasons**
- C2: A.2.3 says only that "some implementations" reserve `fortran` and `asm`.
- C3: a fixed, complete keyword list is simple to test.

**Rejected**
- Reserve them: it would stop valid K&R programs from using those names.
- Match keywords by the first eight characters: would turn `registers` into a keyword.

**Revisit when:** you add inline assembly or a foreign-call feature.

---

## D8. Character constants and strings

**Status:** Amended 2026-11-06. The earlier draft rejected multi-character
constants. A.16 says they are permitted.

**Decision:**

| Case                                       | Result                                           |
|--------------------------------------------|--------------------------------------------------|
| One character, e.g. `'x'`                  | its code; for an octal escape the 8-bit pattern is sign-extended (`'\377'` is -1, A.6.1) |
| Two to four characters, e.g. `'ab'`        | accepted with warning W01; each character contributes its 8-bit pattern, the first in the most significant byte (`'ab'` is 0x6162); value wraps to 32 bits |
| More than four characters                  | error L07                                        |
| `''` (empty)                               | error L06                                        |
| Raw newline or end of file inside `'...`   | error L05                                        |
| Raw newline or end of file inside `"..."`  | error L04                                        |
| `\` followed by newline inside a string    | both ignored, the string continues (A.2.5)       |
| `\` followed by an unlisted character      | the backslash is ignored (A.2.4.3); `'\q'` is `q`, `"\8"` is `8` |
| `\ddd` (1 to 3 octal digits) above 255     | error L08                                        |
| `\0` not followed by an octal digit        | NUL                                              |

Escapes: `\n` LF, `\t` HT, `\b` BS, `\r` CR, `\f` FF, `\\`, `\'`, `\ddd`.
`\"` is needed inside a string (A.2.5). A `"` needs no escape inside a character
constant, and `'` needs none inside a string. Adjacent strings are never joined
(that is ANSI behavior), and every string is a distinct object (A.2.5).

**Reasons**
- A.16: "multi-character character constants are permitted", with machine-dependent
  packing. The first-character-most-significant order matches the left-to-right
  machines of D6.
- A.2.4.3: unlisted escapes drop the backslash.
- C5: each error case has its own message.
- Range-checking `\ddd` against the 8-bit char catches typos like `'\777'`.

**Rejected**
- Reject multi-character constants: contradicts A.16.
- Silently truncate an oversized octal escape: hides bugs.
- Make unknown escapes an error: contradicts A.2.4.3.
- Join adjacent strings: not in K&R.

**Revisit when:** a book program uses a construct these rules reject.

---

## D9. White space and source character set

**Status:** Amended 2026-11-06

**Decision:** White space is the blank, the tab, the newline and comments (A.2).
The carriage return `\r` is also accepted as a blank (recorded extension). `\f` and
`\v` are not white space, so they are invalid characters (L01).
The source character set is ASCII (the target machines of A.2.6 are ASCII). Outside
comments, a character above code 127 is an invalid character (L01). Inside comments
it is ignored. Inside a string or character constant it is L02.
Line and column numbers start at 1. A tab counts as one column. A line break is
`\n`; the `\r` of `\r\n` counts as a blank.

**Reasons**
- C2: the book lists only blanks, tabs and newlines.
- Source files with Windows line endings would otherwise fail on every line.
- C5: precise, simple position rules make error positions predictable and testable.

**Rejected**
- Strict reading (reject `\r`): breaks files from many editors, for no benefit.
- Accept `\f` and `\v`: not needed, and each extension must be recorded.
- Accept any Unicode in constants: the book has no such notion.

**Revisit when:** a book example contains a form feed.

---

## D10. Identifier significance: first 8 characters

**Status:** Active. Supersedes D4.

**Decision:** The first 8 characters of an identifier are significant, as in A.2.2.
The limit is one named constant in one place.

- The lexer keeps the full spelling in the token. It does not truncate.
- The symbol table builds its lookup key from the first 8 characters.
- When two different spellings in the same class and scope share the same first 8
  characters, the compiler emits warning W02 naming both spellings, and the second
  declaration is a redeclaration error.
- The external-name limits in A.2.2 (6 to 8 characters, with case rules, for various
  assemblers and loaders) are not enforced: they describe old linkers.
- Keywords are matched on the full spelling (D7).

**Reasons**
- C2: it follows the book.
- C6: keeping the spelling in the token gives good messages, and only the symbol
  table needs to know about the limit.
- Because the limit is one constant, lifting it later is a one-line change.

**Rejected**
- All characters significant (the old D4): changes program meaning.
- Silent truncation with no warning: collisions give confusing errors.
- Truncating in the lexer: loses the spelling for error messages.
- Enforce external-name limits: tied to hardware that no longer exists.

**Revisit when:** a book program relies on truncated names being equal, or you add
support for linking against other C code.

---

## D11. Plain `char` is signed

**Status:** Amended 2026-11-06. Confirmed by A.6.1; corrected by A.8.2.

**Decision:** Plain `char` is signed, from -128 to 127. A character constant with an
octal escape is sign-extended: `'\377'` is -1. Members of the standard (ASCII)
character set are non-negative. There is no `unsigned char` and no `signed` keyword
(see D15).

**Reasons**
- A.6.1: char signedness is machine-dependent; the PDP-11 sign-extends, and the
  book's own example gives `'\377'` the value -1.
- The choice is one setting in the type model, so changing it later is cheap.

**Rejected**
- Unsigned plain char: equally valid, but makes the book's `'\377'` example differ.
- Add `unsigned char`: A.8.2 does not list it among the acceptable combinations.

**Revisit when:** a book program assumes unsigned char behavior.

---

## D12. Distinct types, even when widths are equal

**Status:** Active

**Decision:** short, int and long are three distinct types in the compiler's type
system, as are float and double, even where D6 gives two of them the same width
(int and long are both 32 bits).

**Reasons**
- A.4 allows an implementation to make them equivalent, but does not require it.
- C6: with distinct types, moving long to 64 bits changes one table entry, not the
  type checker or the symbol table.
- A.6.6 has a separate step for long, so the conversion rules need the distinction.

**Rejected**
- Treat long as an alias of int under D6: simpler today, but the 64-bit switch
  becomes a rewrite and type errors are hidden.

**Revisit when:** never, unless the type system is redesigned.

---

## D13. `register` is a hint

**Status:** Amended 2026-11-06 (rules from A.8.1, A.10.1, A.16 added)

**Decision:** `register` is accepted on any type, where A.8.1 allows it. In stages
1 to 4 a register variable is allocated like an automatic one. The rules the book
attaches to register variables are checked:
- the unary `&` operator cannot be applied to a register variable (A.8.1), whether
  or not it would have been placed in a register;
- `register` is not allowed on external definitions (A.10.2);
- on a function parameter it is the only storage class allowed (A.10.1).
Excess or unsuitable `register` declarations are silently treated as `auto` (A.16).

**Reasons**
- A.8.1: a register declaration is "an auto declaration, together with a hint".
- C1: register allocation is an optimization, and belongs to a later stage.

**Rejected**
- Real register allocation in the early stages: large work, no change in meaning.
- Reject `register`: breaks book programs that use it.

**Revisit when:** you add an optimizer or a register allocator.

---

## D14. Grammar notation

**Status:** Active

**Decision:** The grammar is written in plain-text EBNF using the mapping in
`grammar.md`: syntactic categories in lowercase-hyphen names, literals in quotes,
`[ ]` for the book's "opt" subscript, `{ }` for repetition, `|` for alternatives.
`{ }` and the prefix/suffix layering of declarators are transformations of the
book's recursive rules, allowed only as mechanical rewrites and marked in
`grammar.md`.

**Reasons**
- C3: one notation, written once, keeps every rule consistent and testable.
- The book's italic and bold have no plain-text form, and the mapping is one to one.

**Rejected**
- Reproduce the book's typography with HTML or LaTeX: harder to write and diff.
- Use new notation freely: it would hide differences from the book's grammar.

**Revisit when:** the parser design needs a different shape. Record it as a
separate, labeled transformation.

---

## D15. Type specifier combinations; no `unsigned long`

**Status:** Amended 2026-11-06. Strengthened by A.8.2.

**Decision:** A declaration has at most one type-specifier, except for these
combinations, which A.8.2 lists as acceptable:

| Written                | Meaning                        |
|------------------------|--------------------------------|
| `short`, `short int`   | short                          |
| `long`, `long int`     | long                           |
| `unsigned`, `unsigned int` | unsigned                   |
| `long float`           | double                         |

All other combinations are errors: `unsigned char`, `unsigned short`,
`unsigned long`, `long double`, `short long`, `int int`, `char int`, and so on.
A missing type-specifier means `int`. There is no `void`, `signed`, `const`,
`volatile` or `enum`.

**Reasons**
- A.8.2 lists exactly four combinations and says "otherwise, at most one
  type-specifier may be given".
- A.4 says unsigned long is not supported on the PDP-11, and A.6.6 has no rule for
  combining unsigned long with other types.
- C3: a closed table is simple to test.

**Rejected**
- Support unsigned long with an invented conversion rule: the book makes none.
- Accept `unsigned char` and `unsigned short`: not among the acceptable combinations.

**Revisit when:** a book program needs one of the rejected combinations.

---

## D16. Float to integer conversion truncates toward zero

**Status:** Active

**Decision:** Converting a floating value to an integral type truncates toward zero.
If the value does not fit the destination, the result is undefined (A.6.3).

**Reasons**
- A.6.3 says the direction is machine-dependent, so the compiler must pick one.
- Truncation toward zero is what nearly every instruction set does.
- C3: one fixed rule is testable.

**Rejected**
- Round to nearest: not what the book describes.
- Define out-of-range results: no benefit, and the book leaves them undefined.

**Revisit when:** the code-generation target is chosen.

---

## D17. Expression grammar is layered by precedence

**Status:** Active

**Decision:** The expression grammar has one rule per precedence level (15 levels,
see `grammar.md`), instead of the book's ambiguous `binop` rule. Postfix operators
(`[ ]`, call, `.`, `->`, `++`, `--`) form the tightest level, so `*p++` is `*(p++)`.
The `lvalue` category is not syntactic: the parser accepts any unary expression on
the left of an assignment and the semantic phase checks it is an lvalue.

**Reasons**
- A.7 says precedence is the order of the subsections, so the book's grammar needs
  that outside information to be unambiguous.
- C6: a layered grammar maps directly to recursive descent or precedence climbing.
- C3: each level is a separate rule with its own tests.

**Rejected**
- Copy the book's rules and resolve ambiguity with a table in the parser: the grammar
  document would no longer be a complete definition of the language.
- Place postfix `++` with the right-to-left unary operators: accepts the same programs
  but makes `*p++` need a special case.

**Revisit when:** the parser design is chosen. A precedence-climbing parser may read
the table in `grammar.md` instead; the grammar stays the specification either way.

---

## D18. Compound assignment operators are single tokens

**Status:** Active. Restriction of A.7.14.

**Decision:** `+=`, `-=`, `*=`, `/=`, `%=`, `>>=`, `<<=`, `&=`, `^=` and `|=` are each
one token. The spaced form `x + = 1` is rejected, because `+` followed by `=` is not
a valid expression.

**Reasons**
- A.7.14 says the two parts "are separate tokens", which, read literally, allows
  blanks between them.
- C1 and C6: one token per operator means the parser and AST carry a single operator
  each, and no adjacency check is needed.
- A.2's longest-match rule already makes `==`, `<=` and `>>` single tokens.

**Rejected**
- Lex as two tokens and let the parser pair them: faithful, but pushes a lexical
  decision into the grammar.
- Merge `+` and `=` across white space in the lexer: needs lookahead through blanks
  and comments.

**Revisit when:** a book program uses a spaced compound operator.

---

## D19. Evaluation order

**Status:** Active

**Decision:** The book leaves the order undefined, so the compiler picks one:
- Operands of binary operators and function arguments are evaluated left to right.
  A.16 says arguments are evaluated right to left on the PDP-11 and left to right on
  the other machines, and D6 follows the other machines.
- `&&`, `||`, `?:` and `,` follow A.7.11 to A.7.15 exactly.
- The compiler does not rearrange `*` `+` `&` `|` `^` in the early stages.

**Reasons**
- A.7 allows any order, so a fixed order is always correct.
- C3: deterministic output makes expression tests exact.
- A later optimizer may use the freedom A.7 grants.

**Rejected**
- Leave unspecified: untestable.
- Right to left for arguments: matches the PDP-11, but not the D6 model.

**Revisit when:** an optimizer is added.

---

## D20. Machine-dependent arithmetic

**Status:** Active

**Decision:** Choices where A.6 and A.7 say "machine-dependent":

| Area                             | Choice                                                    |
|----------------------------------|-----------------------------------------------------------|
| Integer overflow                 | wraps in two's complement, no check (A.7)                 |
| Division and `%` by zero         | in a constant expression: compile error; at run time: not checked, host behavior |
| Integer division, negatives      | truncates toward zero; remainder has the sign of the dividend (A.7.3) |
| Right shift                      | arithmetic for signed left operands, logical for unsigned (A.7.5) |
| Shift count negative or >= width | undefined; a constant count out of range gives a warning  |
| Floating-point exceptions        | not handled; host behavior                                |

**Reasons**
- Every row matches what the book says its machines do, or what A.7 allows.
- C3: each is a testable rule, and none needs run-time checks.
- Consistent with D16.

**Rejected**
- Run-time checks for overflow or divide by zero: not in the book.
- Logical right shift for signed values: A.7.5 says arithmetic is what the PDP-11 does.

**Revisit when:** the code-generation target is chosen.

---

## D21. Pointer and integer mixing

**Status:** Active

**Decision:** The following are accepted, and each produces warning W03
("nonportable conversion"), except where the integer is the constant 0:
- assigning an integer to a pointer, or a pointer to an integer,
- assigning a pointer to a pointer of another type (a pure copy),
- comparing a pointer with an integer.
Assigning or comparing the constant 0 with a pointer is silent, and 0 is the null
pointer (A.7.14, A.7.7). Arithmetic combinations A.7.4 does not allow are errors.

**Reasons**
- A.7.14 says compilers allow these copies, and book programs may depend on them (C4).
- C5: a warning flags the nonportable use without breaking fidelity.

**Rejected**
- Reject all: breaks fidelity and some book programs.
- Accept silently: hides errors the book itself calls nonportable.

**Revisit when:** a book program produces a warning that is clearly a false alarm.

---

## D22. Source priority: detailed sections over the summary

**Status:** Active

**Decision:** A.18 says it is "intended more for aiding comprehension than as an
exact statement of the language". When A.18 disagrees with the detailed sections,
the detailed sections win. The known disagreements and their resolutions:

| Topic                       | A.18 says                          | Detailed section says                        | Follow |
|-----------------------------|------------------------------------|----------------------------------------------|--------|
| Conditional `?:` grouping   | "binary operators and the conditional operator all group left-to-right" | A.7.13: right-to-left | A.7.13 |
| Function definition specifiers | `type-specifier` optional       | A.10.1: decl-specifiers, with `extern` or `static` allowed | A.10.1 |
| Function body               | `type-decl-list function-statement`, statement list required | A.10.1: `declaration-list compound-statement` (all optional) | A.10.1 |
| Data definition             | `extern` or `static`, type optional | A.10.2: a declaration, storage class `extern` or `static` | A.10.2 |
| Postfix `++` `--`           | grouped with the right-to-left unary operators | A.7.2 | see D17 |

**Reasons**
- C7 requires one source of truth per rule, and A.18 itself disclaims exactness.
- The detailed sections state meaning; the summary only shows shape.

**Rejected**
- Follow A.18: would make `a ? b : c ? d : e` group the wrong way.

**Revisit when:** a new disagreement is found. Add a row to the table.

---

## D23. Name classes

**Status:** Active

**Decision:** There are exactly two name classes (A.8.5, A.11.1):

| Class | Contains                                                          |
|-------|-------------------------------------------------------------------|
| 1     | ordinary identifiers: variables, functions, parameters, `typedef` names, and statement labels |
| 2     | structure and union members and tags, in one shared class (D24)   |

The two classes are disjoint and never conflict. Members and tags follow the same
scope rules as other identifiers (A.11.1).

**Reasons**
- A.11.1 says ordinary variables on one hand and members and tags on the other form
  two disjoint classes, and that `typedef` names are in the same class as ordinary
  identifiers.
- A.9.12 says a label's scope excludes sub-blocks where the same identifier has been
  redeclared, which only makes sense if labels share the ordinary class. The book
  does not say this directly, so this part is an interpretation.
- This corrects the earlier plan of four separate spaces.

**Rejected**
- Separate classes for tags, members and labels (the modern C model): not K&R.

**Revisit when:** you implement `goto` (stage 5) and find the label rule unworkable.

---

## D24. Member names are one global class

**Status:** Active

**Decision:** Structure and union member names and tags share one class (D23).
Tags and members must be mutually distinct (A.8.5). The same member name may appear
in two different structures only if it has the same type and the same offset in
both; otherwise it is an error ("member redeclared with a different type or offset").
A structure or union may not contain an instance of itself, only a pointer to one.

**Reasons**
- A.8.5: "names of tags and members must be mutually distinct", and the compiler
  "checks only that a name in two different structures has the same type and offset
  in both".
- C6: this global rule is what makes the A.14.1 member-lookup-by-name escape
  possible, so the symbol table must keep one entry per member name.

**Rejected**
- Per-structure member scopes (modern C): easier, but not K&R.

**Revisit when:** book programs from stage 3 are written. If a program violates the
rule, relax it by a new decision.

---

## D25. Implicit declarations

**Status:** Active

**Decision:** The rules of A.13 and A.7.1:
- In an external definition, a missing storage class is `extern`. In a function, a
  missing storage class is `auto`, except that a function is never automatic: a
  declaration of type "function returning ..." is `extern`.
- A storage class with no type means `int`; a missing type means `int`.
- Formal parameters and structure members get their storage class from context.
- An identifier that is not already declared and is followed immediately by `(` is
  implicitly declared as an `extern` function returning `int`. The declaration is
  entered in the file scope (the book does not say where). A later explicit
  declaration of the same name with a different type is an error.

**Reasons**
- A.13 and A.7.1 state the rules directly; the book's hello world depends on the
  last one.
- Entering the implicit declaration at file scope matches A.11.2: all references to
  the same external identifier are references to the same object.
- C6: the symbol table must support implicit entries and later compatibility checks.

**Rejected**
- Require declarations: breaks nearly every program in the book.
- Enter the declaration in the block scope: gives the same meaning but repeats the
  declaration in every block.

**Revisit when:** a book program defines a function after using it with a different type.

---

## D26. Struct and union operations are strict

**Status:** Active. Restriction of A.14.1.

**Decision:** The only operations allowed on a structure or union are naming a member
with `.` and taking its address with `&`. Assigning from or to one, and passing one as
a parameter, is an error (A.14.1). A.14.1's escape (any lvalue before `.`, any pointer
or integer before `->`) is not implemented: the left operand of `.` must be a
structure or union lvalue, and of `->` a pointer to one.

**Reasons**
- A.14.1 states the two permitted operations and says other operations "draw an
  error message".
- The escape is called non-portable and "not firmly enforced"; the book gives no rule
  to implement, only an allowance.
- C1: strict checking is simpler.

**Rejected**
- Implement the escape: possible given D24, but adds type-unsafe cases.

**Revisit when:** a book program uses the escape.

---

## D27. Preprocessor is a separate pass, stage 6

**Status:** Active

**Decision:** The preprocessor of A.12 (`#define`, `#undef`, `#include`, `#if`, `#ifdef`,
`#ifndef`, `#else`, `#endif`, `#line`) is a separate pass that runs before the lexer,
in a new package `src/preprocessor`. Until stage 6 the lexer treats `#` as an invalid
character (L01) and programs must not use control lines.
When it is built: control lines begin with `#`; text inside strings and character
constants is not substituted; a long line is continued with a trailing `\`;
`#include` files may nest (A.12.2).

**Reasons**
- A.12 says these lines "have syntax independent of the rest of the language".
- C1: the lexer stays small and is finished first.
- C6: a separate pass keeps macros out of the parser.

**Rejected**
- Handle control lines inside the lexer: couples two grammars.
- Skip the preprocessor forever: many book programs use `#define`.

**Revisit when:** a stage 1 to 5 book program needs `#define` earlier. Then move the
minimum, object-like `#define`, forward.

---

## D28. Constant expressions

**Status:** Active

**Decision:** Constant expressions (A.15) are used after `case`, as array bounds, in
initializers and after `#if`. Syntactically they are conditional-expressions. The
semantic phase checks they are constant:
- After `case`, as array bounds and in `#if`: integer constants, character constants
  and `sizeof` expressions, joined by the binary operators
  `+ - * / % & | ^ << >> == != < > <= >=`, the unary operators `-` and `~`, and
  the ternary `?:`, with parentheses for grouping, but not function calls.
  `&&`, `||`, `!`, floating constants, assignment and comma are not allowed.
- In initializers of static and external objects: the above, or the address of a
  previously declared external or static object plus or minus a constant (unary `&`
  on such objects, on subscripted external or static arrays, and the implicit address
  of unsubscripted arrays and functions).
- Automatic and register scalars may use arbitrary expressions (A.8.6).
- Values are computed in the D6 integer model. Division by zero is an error (D20).

**Reasons**
- A.15 lists the permitted operators exactly.
- C3: a closed list is simple to test.

**Rejected**
- Allow `&&`, `||` and `!`: not in A.15's lists.

**Revisit when:** a book program uses `&&` in a `case` or array bound.

---

## D29. Old-style function definitions only

**Status:** Active

**Decision:** Function definitions use the K&R form (A.10.1):
`type_opt name(param, ...) declarations { body }`.
- The parameter list holds identifiers only; the type of each is declared between
  the `)` and the `{`; an undeclared parameter is `int`.
- Only identifiers in the parameter list may be declared there, and the only storage
  class allowed is `register`.
- Declared `float` parameters are adjusted to `double`; declared "array of ..."
  parameters are adjusted to "pointer to ...".
- A function declarator in a declaration carries no parameter list: `int f();`.
- There are no prototypes. Calls are not checked against parameters (A.7.1).
- A function body may be empty (D22).

**Reasons**
- C2: prototypes are ANSI C.
- A.10.1 states the adjustments exactly.

**Rejected**
- Accept prototypes as an extension: changes the language.

**Revisit when:** never within K&R scope.

---

## D30. Bit-fields are deferred

**Status:** Active

**Decision:** Bit-fields (A.8.5) are stage 5. The grammar rules are written now.
When built: fields are packed into machine words (32 bits) without straddling words,
are assigned left to right (D6), are unsigned, hold only integer values, cannot be
arrays, cannot have `&` applied (so no pointers to fields), and a zero-width unnamed
field aligns the next field to a word boundary.

**Reasons**
- C1: bit-fields add layout work that nothing else in the early stages needs.
- A.8.5 says implementations need not support anything but integer fields.

**Rejected**
- Reject bit-fields forever: contradicts C2.

**Revisit when:** stage 5 begins.

---

## D31. Anachronisms are not supported

**Status:** Active

**Decision:** The obsolete forms of A.17 are not supported:
- The `=op` assignment form. With longest match, `x=-1` lexes as `x`, `=`, `-`, `1`
  and means "assign -1 to x".
- Initializers without the `=` sign (`int x 1;`).

**Reasons**
- A.17 says these "will disappear", and the new forms are the standard of the book.
- C3: no ambiguity to test around.

**Rejected**
- Support `=-` as a decrement: produces the surprising meaning A.17 itself warns about.

**Revisit when:** a book program uses an obsolete form.

---

## D32. Interpretations of ambiguous wording

**Status:** Active

**Decision:** Places where the book's wording leaves a choice:

| Topic                              | Interpretation                                           |
|------------------------------------|----------------------------------------------------------|
| `%` operands "must not be float" (A.7.3) | both operands must be integral; floating operands are an error |
| Cast result                        | not an lvalue                                            |
| `sizeof` result                    | type int, an integer constant (A.7.2)                    |
| `switch` expression (A.9.7)        | after the usual conversions its type must be int; a long expression is an error |
| `case` constant (A.9.7)            | must be an int constant expression; duplicates are an error |
| Array bound (A.8.4)                | an int constant expression greater than zero, except that the first bound may be omitted |
| `sizeof` on a function or incomplete type | error                                             |
| Block-level declaration order (A.9.2) | declarations come before statements in a block        |

**Reasons**
- C3: each choice is a testable rule, and each follows the book's wording as literally
  as it allows.

**Rejected**
- Leave open: untestable.

**Revisit when:** a book program contradicts one of the interpretations.

---

## History: corrections made on finishing Appendix A (2026-11-06)

Earlier drafts of this log (and of the discussion around them) were checked against
the full text of Appendix A. These items were wrong or incomplete and are corrected
above:

1. **Multi-character constants** are permitted by A.16. The earlier D8 rejected them.
2. **`unsigned char` and `unsigned short`** are not among the combinations A.8.2 lists,
   so they are not K&R C. The earlier D11 wrongly offered `unsigned char`.
3. **Name classes:** there are two (ordinary names and typedef names; members and
   tags), not four. See D23 and D24.
4. **Conditional operator grouping:** A.18 contradicts A.7.13. See D22.
5. **Constant typing:** an octal or hex constant is long only above the largest
   unsigned int. See D6.
6. **D6 widths** were checked against the A.2.6 table and confirmed.
7. **`&&`, `||` and `!`** are not allowed in constant expressions (A.15). See D28.
8. **Dates:** earlier entries were dated 2026-11-04. All entries now carry the date
   the project documents were finalized.
