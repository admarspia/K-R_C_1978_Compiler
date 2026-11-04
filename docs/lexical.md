# Lexical Conventions (K&R Appendix A, section 2)

Six token classes: identifiers, keywords, constants, strings, operators,
other separators. Source: Appendix A section 2.

## 2.0 General rules
- White space (blanks, tabs, newlines, comments) is ignored except to
  separate tokens.
- Longest match: the next token is the longest character string that can
  form a token.

## 2.1 Comments
    comment = "/*" { any-char } "*/"
- Comments do not nest. They count as white space.
- Valid:   `x = 1; /* set x */`
- Invalid: `/* never closed` -> error: unterminated comment (line:col of `/*`)

## 2.2 Identifiers
    letter     = "a".."z" | "A".."Z" | "_"
    digit      = "0".."9"
    identifier = letter { letter | digit }
- Case-sensitive.
- First 8 characters significant (see decisions D4).

## 2.3 Keywords
int char float double struct union long short unsigned auto extern
register typedef static goto return sizeof break continue if else for
do while switch case default entry
- `entry` is reserved and unused.
- `fortran` and `asm`: see decisions.

## 2.4 Constants
    digits            = digit { digit }
    octal-digit       = "0".."7"
    hex-digit         = digit | "a".."f" | "A".."F"
    decimal-constant  = ("1".."9") { digit }
    octal-constant    = "0" { digit }
    hex-constant      = "0" ("x" | "X") hex-digit { hex-digit }
    long-suffix       = "l" | "L"
    integer-constant  = (decimal-constant | octal-constant | hex-constant)
                        [ long-suffix ]

- Leading 0 means octal; the digits 8 and 9 are accepted and have values
  10 and 11 (octal).
- A decimal constant above the largest signed int is long. An octal or hex
  constant above the largest unsigned int is long. (Needs decision D6.)

    exponent          = ("e" | "E") [ "+" | "-" ] digits
    floating-constant = digits "." [ digits ] [ exponent ]
                      | "." digits [ exponent ]
                      | digits exponent
- Every floating constant is double. There is no suffix.
- Valid: `25.`  `.5`  `1e5`  `2.5e-3`
- Invalid: `.` alone is not a constant. `1e` and `1e+` -> malformed number.

    escape        = "\" ( "n" | "t" | "b" | "r" | "f" | "\" | "'"
                        | octal-digit [ octal-digit [ octal-digit ] ] )
                  | "\" any-other-char     (backslash ignored)
    char-constant = "'" ( char | escape ) "'"
- Value is the machine character code.
- Escapes: \n newline, \t tab, \b backspace, \r return, \f form feed,
  \\ backslash, \' quote, \ddd octal (1 to 3 digits), \0 is NUL.

## 2.5 Strings
    string = '"' { char | escape | "\" newline } '"'
- Type array of char, static storage, NUL appended.
- `\"` for a quote. A backslash followed by newline is ignored.
- All strings are distinct, even if written identically.
