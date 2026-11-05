# Grammar Notation

| Book (Appendix A section 3)            | This project              |
|----------------------------------------|---------------------------|
| syntactic category (italic)            | lowercase-hyphen name     |
| literal word or character (bold)       | quoted: "while", "+="     |
| alternatives on separate lines         | alternatives joined by |  |
| optional symbol (subscript opt)        | [ ... ]                   |

Rule form:  name = alternative | alternative ;

Extension (D14): the book has no repetition notation and writes lists
recursively. `{ ... }` (zero or more) may be used as a mechanical rewrite
of such a recursive rule. Each use is marked with the book rule it replaces.

## Expressions (A.7)

Source: A.7. Precedence is the order of the subsections, highest first.
The book's rules are ambiguous without that ordering, so the grammar is written
one rule per precedence level (D17). The `lvalue` category of the book is not
a syntactic category here: the parser accepts any unary-expression on the left of an
assignment, and the semantic phase checks that it is an lvalue.

```
expression                = assignment-expression { "," assignment-expression } ;

assignment-expression     = conditional-expression
                          | unary-expression assignment-operator assignment-expression ;
assignment-operator       = "=" | "+=" | "-=" | "*=" | "/=" | "%="
                          | ">>=" | "<<=" | "&=" | "^=" | "|=" ;

conditional-expression    = logical-or-expression
                            [ "?" expression ":" conditional-expression ] ;

logical-or-expression     = logical-and-expression { "||" logical-and-expression } ;
logical-and-expression    = inclusive-or-expression { "&&" inclusive-or-expression } ;
inclusive-or-expression   = exclusive-or-expression { "|" exclusive-or-expression } ;
exclusive-or-expression   = and-expression { "^" and-expression } ;
and-expression            = equality-expression { "&" equality-expression } ;
equality-expression       = relational-expression { ( "==" | "!=" ) relational-expression } ;
relational-expression     = shift-expression { ( "<" | ">" | "<=" | ">=" ) shift-expression } ;
shift-expression          = additive-expression { ( "<<" | ">>" ) additive-expression } ;
additive-expression       = multiplicative-expression { ( "+" | "-" ) multiplicative-expression } ;
multiplicative-expression = unary-expression { ( "*" | "/" | "%" ) unary-expression } ;

unary-expression          = postfix-expression
                          | ( "*" | "&" | "-" | "!" | "~" | "++" | "--" ) unary-expression
                          | "sizeof" unary-expression
                          | "sizeof" "(" type-name ")"
                          | "(" type-name ")" unary-expression ;

postfix-expression        = primary-expression
                            { "[" expression "]"
                            | "(" [ argument-list ] ")"
                            | "." identifier
                            | "->" identifier
                            | "++" | "--" } ;

primary-expression        = identifier | constant | string | "(" expression ")" ;

argument-list             = assignment-expression { "," assignment-expression } ;
```

Notes:
- `argument-list` uses assignment-expression, not expression: in argument lists and
  initializer lists a comma is a separator, and the comma operator may appear only
  inside parentheses (A.7.15). `f(a, (t=3, t+2), c)` has three arguments.
- `sizeof ( type-name )` is a unit: `sizeof (int) - 2` means `(sizeof (int)) - 2`.
  After `sizeof (` or a bare `(`, the parser decides between a type-name and an
  expression by the next token (a type keyword, `struct`, `union`, or later a
  typedef name).
- The book lists postfix `++` and `--` with the unary operators (A.7.2). Putting
  them with the postfix operators accepts the same programs. Verify against A.18.
- The third operand of `?:` is written as conditional-expression to give
  right-to-left grouping. Verify against A.18.

### Precedence and associativity

| Level | Operators                                   | Associativity | Book |
|-------|---------------------------------------------|---------------|------|
| 1     | `( )` call, `[ ]`, `.`, `->`                | left to right | 7.1  |
| 2     | `*` `&` `-` `!` `~` `++` `--` `sizeof` cast | right to left | 7.2  |
| 3     | `*` `/` `%`                                 | left to right | 7.3  |
| 4     | `+` `-`                                     | left to right | 7.4  |
| 5     | `<<` `>>`                                   | left to right | 7.5  |
| 6     | `<` `>` `<=` `>=`                           | left to right | 7.6  |
| 7     | `==` `!=`                                   | left to right | 7.7  |
| 8     | `&`                                         | left to right | 7.8  |
| 9     | `^`                                         | left to right | 7.9  |
| 10    | `|`                                         | left to right | 7.10 |
| 11    | `&&`                                        | left to right | 7.11 |
| 12    | `||`                                        | left to right | 7.12 |
| 13    | `?:`                                        | right to left | 7.13 |
| 14    | `=` `+=` `-=` `*=` `/=` `%=` `>>=` `<<=` `&=` `^=` `|=` | right to left | 7.14 |
| 15    | `,`                                         | left to right | 7.15 |
