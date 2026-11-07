# Language Specification: Overview and Scope

The language is K&R C, as defined by Appendix A (C Reference Manual) of
The C Programming Language, 1st edition, 1978 (D1). This file gives the scope, the
stages and the map of the other documents. The rules themselves are in the documents
listed in section 1.

Last updated: 2026-11-06

## 1. Document map

| Document              | Contents                                                              |
|-----------------------|-----------------------------------------------------------------------|
| `constraints.md`      | C1 to C7: what decisions are judged against                           |
| `decisions.md`        | D1 to D32: every choice, with reasons and rejected options            |
| `lexical.md`          | tokens, constants, strings, operators, errors L01 to L13, warning W01 |
| `grammar.md`          | EBNF for the whole language, precedence table                         |
| `semantics.md`        | storage classes, types, conversions, expressions, declarations, statements, scope |
| `symbol-table.md`     | design of the symbol table (no code)                                  |
| `test-vectors.md`     | cases for every rule                                                  |

Reading order for implementers: `lexical.md`, `grammar.md`, `semantics.md`,
`symbol-table.md`. Check `decisions.md` for any rule marked with a D-number.

## 2. Conformance

- A program is valid if it follows `grammar.md` and `semantics.md` for the stage being
  compiled.
- The compiler reports every error with line and column, and every warning, in source
  order (C5, D5).
- Where Appendix A says "machine-dependent" or "undefined", the choice is in `decisions.md`
  (D6, D8, D11, D16, D19, D20).
- Where the detailed sections and the A.18 summary disagree, the detailed sections win (D22).
- Extensions and restrictions beyond the book: `\r` as a blank (D9), compound assignment as
  single tokens (D18), pointer and integer mixing warnings (D21), strict struct operations
  (D26), constant-expression operator list (D28), the identifier collision warning (D10).

## 3. Stages (D2)

| Stage | Adds                                                                          | Examples that should work |
|-------|-------------------------------------------------------------------------------|---------------------------|
| 1     | `int`, `char`; all operators on them; `if`, `while`, `for`, `return`, `break`, `continue`, null and compound statements; old-style functions; `auto` `static` `extern` `register`; scalar initializers; implicit declarations; casts to `int` and `char`; `sizeof` | small arithmetic and loop programs, recursive functions |
| 2     | pointers, arrays, string literals, subscripting, unary `*` and `&`, pointer arithmetic, brace initializers, type names with `*` `[ ]` `( )` | string handling, array programs, "hello, world" |
| 3     | `struct`, `.` and `->`, tags, self-referential structures                     | linked structures, the `tnode` example |
| 4     | `short`, `long`, `unsigned`, `float`, `double`, floating arithmetic           | floating-point programs |
| 5     | `switch`, `case`, `default`, `do`, `goto`, labels, `union`, `typedef`, bit-fields | the remaining statements and type features |
| 6     | the preprocessor: `#define`, `#undef`, `#include`, `#if`, `#ifdef`, `#ifndef`, `#else`, `#endif`, `#line` | programs that use macros and include files |

The lexer is complete from the first stage. A construct from a later stage is reported as
"not supported yet (stage N)" by the earliest phase that recognizes it.

## 4. Scope by section of Appendix A

| A.   | Section                     | Status                                                   | Stage |
|------|-----------------------------|----------------------------------------------------------|-------|
| 1    | Introduction                | not a language rule                                      | —     |
| 2    | Lexical conventions         | implemented in full: `lexical.md`                        | 1     |
| 2.6  | Hardware characteristics    | model chosen: D6                                         | 1     |
| 3    | Syntax notation             | notation of `grammar.md`: D14                            | —     |
| 4    | Names: storage class, type  | `char`, `int`: stage 1; pointers, arrays: 2; `struct`: 3; `short`, `long`, `unsigned`, `float`, `double`: 4; `union`: 5 | 1 to 5 |
| 5    | Objects and lvalues         | implemented                                              | 1     |
| 6    | Conversions                 | 6.1, 6.6 for int: 1; 6.4: 2; 6.2, 6.3, 6.5: 4            | 1 to 4 |
| 7.1  | Primary expressions         | identifiers, constants, parentheses, calls: 1; subscripts, strings: 2; `.` `->`: 3 | 1 to 3 |
| 7.2  | Unary operators             | all but `*` `&`: 1; `*` `&`: 2                           | 1, 2  |
| 7.3 to 7.15 | Binary, conditional, assignment, comma | implemented on int and char: 1; pointer forms: 2; floating forms: 4 | 1 to 4 |
| 8.1  | Storage class specifiers    | `auto` `static` `extern` `register`: 1; `typedef`: 5     | 1, 5  |
| 8.2  | Type specifiers             | `char` `int`: 1; others by stage; combinations: D15      | 1 to 5 |
| 8.3, 8.4 | Declarators             | identifier, function, parentheses: 1; `*` `[ ]`: 2       | 1, 2  |
| 8.5  | Structures and unions       | `struct`: 3; `union`, bit-fields: 5                      | 3, 5  |
| 8.6  | Initialization              | scalar: 1; aggregate and string: 2; structure: 3         | 1 to 3 |
| 8.7  | Type names                  | `int` `char`: 1; derived: 2                              | 1, 2  |
| 8.8  | Typedef                     | stage 5; needs the parser to query the symbol table      | 5     |
| 9.1 to 9.6, 9.9, 9.10, 9.13 | Expression, compound, `if`, `while`, `for`, `break`, `continue`, `return`, null | implemented | 1 |
| 9.5  | `do`                        | stage 5                                                  | 5     |
| 9.7  | `switch`                    | stage 5                                                  | 5     |
| 9.8  | `break`                     | stage 1 in loops; in `switch` with stage 5               | 1, 5  |
| 9.11, 9.12 | `goto`, labels        | stage 5                                                  | 5     |
| 10   | External definitions        | implemented; old-style functions: D29                    | 1     |
| 11   | Scope rules                 | implemented; typedef scope: stage 5                      | 1, 5  |
| 12   | Compiler control lines      | the preprocessor: D27                                    | 6     |
| 13   | Implicit declarations       | implemented: D25                                         | 1     |
| 14   | Types revisited             | 14.1: 3; 14.2, 14.3: 2; 14.4: 2 and 4                    | 2 to 4 |
| 15   | Constant expressions        | implemented: D28                                         | 1, 2  |
| 16   | Portability considerations  | choices recorded: D6, D8, D11, D13, D19, D20             | —     |
| 17   | Anachronisms                | not supported: D31                                       | —     |
| 18   | Syntax summary              | reference only: D22                                      | —     |

## 5. Out of scope (not in K&R C 1978)

`void`, `const`, `volatile`, `signed`, `enum`, prototypes, `//` comments, `unsigned char`,
`unsigned short`, `unsigned long`, adjacent-string joining, structure assignment and passing,
`#elif`, `#pragma`, `defined()`, token pasting and stringizing in macros, trigraphs, wide
characters, hexadecimal floating constants, the `u` and `f` suffixes, the `=op` operators.

## 6. Open items

| Item                                                  | Decided when                         |
|-------------------------------------------------------|--------------------------------------|
| code-generation target (assembly, VM, interpreter)    | before stage 2 code generation       |
| layout details for structure and bit-field codegen    | stages 3 and 5                       |
| exact text of semantic error messages                 | with the semantic phase              |
| how book programs get `printf`, `getchar` and the like | when the target is chosen (they are library functions, not language) |
