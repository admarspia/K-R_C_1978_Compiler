# Lexical Specification (K&R Appendix A, section 2)

Source: Appendix A section 2, with the decisions D3, D5 to D11, D18, D27 and D31.
This document is complete: the lexer is written from it.

Last updated: 2026-11-06

## 1. Conventions

**Notation.** Grammar in this file uses the notation of `grammar.md`.
`"x"` is a literal, `[ ]` optional, `{ }` repetition, `|` alternative,
`"a".."z"` a range.

**Source character set.** ASCII (D9).

**Positions.** Line and column numbers start at 1. A tab counts as one column. A line
break is the character `\n`. The `\r` of a `\r\n` pair is a blank. The position of a
token is the line and column of its first character.

**Token classes.** The book names six: identifiers, keywords, constants, strings,
operators and other separators (A.2). This project uses these token kinds:

| Kind        | Notes                                                              |
|-------------|--------------------------------------------------------------------|
| identifier  | spelling kept in full (D10)                                        |
| keyword     | the 28 words of section 4                                          |
| integer constant | value and type (int or long), from section 5.1 and 5.2        |
| floating constant | value (double), section 5.4                                  |
| character constant | value (int), section 5.3                                    |
| string      | decoded characters, section 6                                      |
| operator    | section 7                                                          |
| separator   | `( ) [ ] { } ; ,`                                                  |
| end of file | always produced at the end of input, never `null`                  |

Which punctuation counts as an operator and which as a separator is a project
convention (the book does not list them). The parser uses the exact lexeme, so the
convention does not affect the language.

**Token contents.** Every token has: kind, exact lexeme (source spelling), start line,
start column. Constants also have a value; strings also have their decoded characters.

## 2. White space and comments (A.2, A.2.1)

    white-space = " " | tab | newline | carriage-return | comment ;
    comment     = "/*" { any-character } "*/" ;     (* shortest match, no nesting *)

- White space is ignored except to separate tokens. Some white space is required
  between adjacent identifiers, keywords and constants (see section 9, L12).
- A comment ends at the first `*/` after the opening `/*`. Comments do not nest.
- A comment counts as white space, so `a/**/b` is two tokens.
- `\r` is a blank (D9). `\f`, `\v` and every other control character is invalid (L01).
- `//` is not a comment (D3): it is two `/` operators.
- A `*/` outside a comment is the operators `*` and `/`.

| Input        | Result                                                  |
|--------------|---------------------------------------------------------|
| `a/**/b`     | identifier a, identifier b                              |
| `/*/`        | error L03: the `/` after `/*` does not close the comment|
| `/**/`       | empty comment                                           |
| `/* /* */ */`| comment, then operators `*` and `/`                     |

## 3. Identifiers (A.2.2)

    letter     = "a".."z" | "A".."Z" | "_" ;
    digit      = "0".."9" ;
    identifier = letter { letter | digit } ;

- Case is significant.
- The underscore counts as a letter.
- The token keeps the full spelling. Only the first 8 characters are significant for
  lookup; that rule belongs to the symbol table (D10).
- An identifier is a keyword only if its whole spelling is a keyword (D7).

## 4. Keywords (A.2.3)

```
int      extern    else
char     register  for
float    typedef   do
double   static    while
struct   goto      switch
union    return    case
long     sizeof    default
short    break     entry
unsigned continue
auto     if
```

28 words, all lowercase. `entry` is reserved and unused. `fortran` and `asm` are
ordinary identifiers (D7). `true`, `false`, `void`, `const`, `enum`, `signed` and
`volatile` are ordinary identifiers.

## 5. Constants (A.2.4)

### 5.1 Integer constants

    octal-digit   = "0".."7" ;
    hex-digit     = digit | "a".."f" | "A".."F" ;
    decimal       = ("1".."9") { digit } ;
    octal         = "0" { digit } ;                          (* digits 8 and 9 allowed *)
    hexadecimal   = "0" ( "x" | "X" ) hex-digit { hex-digit } ;
    long-suffix   = "l" | "L" ;
    integer-constant = ( decimal | octal | hexadecimal ) [ long-suffix ] ;

- A constant that starts with `0` is octal, otherwise decimal.
- In an octal constant the digits 8 and 9 have the octal values 10 and 11. Their
  contribution is `digit * 8^position`, so `089` is `0*64 + 8*8 + 9 = 73`.
- A constant starting with `0x` or `0X` is hexadecimal; `a` to `f` (either case) are 10
  to 15. At least one hex digit is required (L10).
- There is no sign: `-1` is the operator `-` and the constant `1`.
- `0` is the octal constant zero.

**Value and type** (D6):

| Form                 | Valid range         | Type | Error above the range |
|----------------------|---------------------|------|-----------------------|
| decimal              | 0 to 2147483647     | int  | L11                   |
| octal, hexadecimal   | 0 to 0xFFFFFFFF     | int (values above 0x7FFFFFFF wrap negative) | L11 |
| any, with `l` or `L` | same limits         | long | L11                   |

### 5.2 Explicit long constants (A.2.4.2)

A `l` or `L` immediately after an integer constant makes it a long constant. No other
suffix exists. Anything else immediately after a number is L12.

### 5.3 Character constants (A.2.4.3)

    char-constant = "'" char-body { char-body } "'" ;
    char-body     = any-character-except-quote-backslash-newline | escape ;
    escape        = "\" ( "n" | "t" | "b" | "r" | "f" | "\" | "'"
                        | octal-digit [ octal-digit [ octal-digit ] ]
                        | any-other-character-except-newline ) ;

| Escape | Meaning         | Value |
|--------|-----------------|-------|
| `\n`   | newline (LF)    | 10    |
| `\t`   | horizontal tab  | 9     |
| `\b`   | backspace       | 8     |
| `\r`   | carriage return | 13    |
| `\f`   | form feed       | 12    |
| `\\`   | backslash       | 92    |
| `\'`   | single quote    | 39    |
| `\ddd` | 1 to 3 octal digits | the value, 0 to 255 (above is L08) |
| `\0`   | NUL (not followed by an octal digit) | 0 |
| `\` + any other character | the backslash is ignored | that character |

- A `\ddd` escape reads as many octal digits as possible, up to three. The digits 8 and
  9 are not octal digits in an escape, so `\8` is `8`.
- A raw newline inside a character constant is L05.
- A `"` needs no escape inside a character constant.

**Value** (D8, D11):

| Constant         | Value                                                       |
|------------------|-------------------------------------------------------------|
| one character    | its code; for an octal escape, the 8-bit pattern sign-extended: `'\377'` is -1 |
| 2 to 4 characters | warning W01; each character's 8-bit pattern, first character in the most significant byte; wraps to 32 bits |
| 5 or more        | error L07                                                   |
| none (`''`)      | error L06                                                   |

The type of every character constant is int (A.7.1).

### 5.4 Floating constants (A.2.4.4)

    digits   = digit { digit } ;
    exponent = ( "e" | "E" ) [ "+" | "-" ] digits ;
    floating-constant
             = digits "." [ digits ] [ exponent ]
             | "." digits [ exponent ]
             | digits exponent ;

- The integer part or the fraction part (not both) may be missing.
- The decimal point or the `e` and exponent (not both) may be missing.
- Every floating constant is double. There is no `f` or `l` suffix.
- Valid: `25.`  `.5`  `1e5`  `1E5`  `2.5e-3`  `1.5e+2`  `3.14`.
- `.` followed by a non-digit is the member-access operator, not a constant.
- An `e` or `E` that is not followed by digits (after an optional sign) is L09.
- A value too large for a double is L13.
- Digits followed by a letter other than a valid exponent (`1.5f`) is L12.
- Floating constants use decimal digits only: `0x1.8` is not a constant (the hex
  constant `0x1` is followed by `.8`).

## 6. Strings (A.2.5)

    string = '"' { string-char } '"' ;
    string-char = any-character-except-quote-backslash-newline
                | escape
                | "\" newline ;

- The same escapes as in character constants may be used, and `\"` stands for a
  double quote. A bare `"` ends the string.
- A `\` immediately followed by a newline is ignored: both characters are dropped and
  the string continues on the next line. The line counter advances.
- A raw newline or end of file before the closing quote is L04.
- Type: array of char; storage class static; a NUL (`\0`) is placed after the last
  character. The token holds the characters without the terminator; the terminator is
  added by later phases.
- A string may contain `\0`. Its length is the number of decoded characters.
- Adjacent strings are not joined. Every string is a distinct object, even when
  written identically.
- A `'` needs no escape inside a string.
- `\ddd` above 255 inside a string is L08.

## 7. Operators and separators

Longest match applies (A.2): at each position the lexer takes the longest string that
forms a token.

**Three-character operators:** `>>=` `<<=`

**Two-character operators:** `++` `--` `->` `<<` `>>` `<=` `>=` `==` `!=` `&&` `||`
`+=` `-=` `*=` `/=` `%=` `&=` `^=` `|=`

**One-character operators:** `+` `-` `*` `/` `%` `&` `|` `^` `~` `!` `<` `>` `=` `?` `:`
`.`

**Separators:** `(` `)` `[` `]` `{` `}` `;` `,`

Roles (the lexer produces one token, the parser decides from position):

| Lexeme | Roles                                                                 |
|--------|-----------------------------------------------------------------------|
| `-`    | subtraction, negation                                                 |
| `*`    | multiplication, indirection, pointer declarator                       |
| `&`    | bitwise AND, address-of                                               |
| `+`    | addition only (there is no unary plus, A.7.2)                         |
| `++` `--` | prefix and postfix increment and decrement                         |
| `,`    | comma operator, and the separator in argument, initializer, declarator and parameter lists |
| `:`    | conditional operator, `case`, `default`, labels, bit-field widths     |
| `=`    | assignment, and the introducer of an initializer                      |
| `.`    | member access (a floating constant when followed by a digit)          |
| `( )`  | grouping, call, cast, declarators                                     |
| `[ ]`  | subscript, array declarators                                          |
| `{ }`  | blocks, initializer lists, struct and union bodies                    |

Compound assignment operators are single tokens (D18). The obsolete `=op` forms do
not exist (D31). The character `#` is invalid until the preprocessor exists (D27).

| Input        | Tokens                                                  |
|--------------|---------------------------------------------------------|
| `a+++++b`    | a `++` `++` `+` b (the parser rejects it)               |
| `a- -b`      | a `-` `-` b                                             |
| `a--b`       | a `--` b                                                |
| `x=-1`       | x `=` `-` 1                                             |
| `x>>=2`      | x `>>=` 2                                               |
| `x >> =2`    | x `>>` `=` 2 (parse error)                              |
| `p->q`       | p `->` q                                                |
| `a<b<=c`     | a `<` b `<=` c                                          |
| `a/b`        | a `/` b                                                 |
| `a//b`       | a `/` `/` b (not a comment, D3)                         |
| `x.5`        | x, floating constant `.5` (parse error)                 |

## 8. Dispatch by first character

When the lexer starts a token (after skipping white space and comments), the first
character decides the kind:

| First character                | Token kind                                      |
|--------------------------------|-------------------------------------------------|
| letter or `_`                  | identifier or keyword                           |
| digit                          | integer or floating constant                    |
| `.` followed by a digit        | floating constant                               |
| `'`                            | character constant                              |
| `"`                            | string                                          |
| `/` followed by `*`            | comment (skipped)                               |
| one of the operator or separator characters | operator or separator              |
| end of input                   | end-of-file token                               |
| anything else                  | L01                                             |

## 9. Adjacent tokens (A.2)

"Some white space is required to separate otherwise adjacent identifiers, keywords,
and constants." Applied by the lexer:

- A number immediately followed by a letter or `_` that is not part of the number
  (a hex digit in a hex constant, an `l` or `L` suffix, or a valid exponent) is
  error L12. Examples: `12ab`, `0x1G`, `12ll`, `1.5f`, `00x1`.
- Two numbers that touch because of the longest-match rule, such as `1.2.3`
  (`1.2` and `.3`), are separate tokens. The parser rejects them.

## 10. Errors and warnings

Every error has a code, a message, the position and a recovery rule (D5). The position
is where the problem starts, as noted in the table.

| Code | Condition                                      | Message                                    | Position     | Recovery                          |
|------|------------------------------------------------|--------------------------------------------|--------------|-----------------------------------|
| L01  | invalid character outside a constant or comment | `invalid character 'c'` (code point for non-printable) | the character | skip that character          |
| L02  | invalid character inside a string or character constant | `invalid character in constant`    | the character | skip that character               |
| L03  | `/*` with no closing `*/`                      | `unterminated comment`                     | the `/*`     | end of file, stop                 |
| L04  | newline or end of file inside a string         | `unterminated string`                      | the opening `"` | skip to end of line            |
| L05  | newline or end of file inside a character constant | `unterminated character constant`      | the opening `'` | skip to end of line            |
| L06  | `''`                                           | `empty character constant`                 | the opening `'` | consume through the closing `'` |
| L07  | more than four characters in a constant        | `character constant too long`              | the opening `'` | consume through the closing `'` |
| L08  | `\ddd` with value above 255                    | `octal escape out of range`                | the `\`      | keep scanning the constant        |
| L09  | exponent with no digits (`1e`, `1e+`)          | `malformed floating constant`              | start of the number | consume the whole literal   |
| L10  | `0x` or `0X` with no hex digit                 | `hexadecimal constant has no digits`       | start of the number | consume the whole literal   |
| L11  | integer constant above its limit               | `integer constant too large`               | start of the number | consume the whole literal   |
| L12  | number followed by a letter or `_`             | `invalid character after number`           | start of the number | consume the whole run of letters, digits and `_` |
| L13  | floating constant above the double range       | `floating constant out of range`           | start of the number | consume the whole literal   |

| Code | Warning                                        | Message                                    |
|------|------------------------------------------------|--------------------------------------------|
| W01  | character constant of 2 to 4 characters        | `multi-character constant is machine-dependent` |

Warnings are reported with line and column and do not stop compilation.
W02 (identifier collision) and W03 (nonportable pointer conversion) belong to later
phases; see `symbol-table.md` and `semantics.md`.

Messages are fixed text so tests can assert on them. Add a new code for each new
error; never reuse a number.

## 11. What the lexer does not do

- It does not decide if an identifier is a typedef name; that needs the symbol table
  (see `symbol-table.md`).
- It does not decide the role of `*`, `&`, `-`, `,` or `:`.
- It does not truncate identifiers (D10).
- It does not join strings or expand macros.
- It does not check that constants fit their later use (`sizeof`, array bounds).
- It does not report the stage-gating error "not supported yet"; that is a parser or
  semantic phase job (D2).

## 12. Verification

Each rule above has at least one case in `test-vectors.md`. A lexer is conforming when
it produces exactly the listed tokens, errors, warnings and positions.
