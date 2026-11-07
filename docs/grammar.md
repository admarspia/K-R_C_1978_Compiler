# Grammar (K&R Appendix A, sections 3, 7 to 10, 12 and 18)

The syntax of the language, written from the detailed sections of Appendix A. Where
the summary in A.18 disagrees with them, the detailed sections win (D22).
Tokens come from `lexical.md`. Meaning and constraints are in `semantics.md`.

Last updated: 2026-11-06

## 1. Notation (A.3, D14)

| Book                                   | This project                          |
|----------------------------------------|---------------------------------------|
| syntactic category (italic)            | lowercase-hyphen name                 |
| literal word or character (bold)       | quoted: `"while"`, `"+="`             |
| alternatives on separate lines         | alternatives joined by `|`            |
| optional symbol (subscript "opt")      | `[ ... ]`                             |

Rule form: `name = alternative | alternative ;`

Additional notation, used only as mechanical rewrites of the book's recursive rules:

| Notation      | Meaning                                |
|---------------|----------------------------------------|
| `{ ... }`     | zero or more repetitions               |
| `( ... )`     | grouping                               |
| `(* ... *)`   | comment                                |

Names written in quotes are tokens. `identifier`, `constant` and `string` are the
token kinds of `lexical.md` (`constant` is an integer, floating or character
constant). The token `end-of-file` ends the input.

## 2. Parsing needs symbol-table information

One decision cannot be made from tokens alone: whether an identifier is a typedef
name. The parser asks the symbol table (stage 5; see `symbol-table.md`).

- An identifier is a `typedef-name` specifier only if it is currently declared as a
  typedef name **and** no type-specifier has yet appeared in the same specifier list
  (A.11.1: after `typedef float distance;`, the declaration `auto int distance;`
  declares a variable, and `auto distance;` declares nothing with type `distance`).
- A parenthesis followed by a type keyword, `struct`, `union` or a typedef name starts
  a cast or a `sizeof` type name. Otherwise it is a parenthesized expression.
- `identifier ":"` at the start of a statement is a label. Two tokens of lookahead
  are enough.

## 3. Program structure (A.10, A.18.4)

```
program               = { external-definition } ;

external-definition   = function-definition | data-definition ;

function-definition   = [ decl-specifiers ] declarator
                        { declaration } compound-statement ;

data-definition       = decl-specifiers [ init-declarator-list ] ";"
                      | init-declarator-list ";" ;
```

Notes:
- A function definition and a data definition both begin with optional specifiers and
  a declarator. After the declarator, a `{` or the start of a declaration (a type or
  storage-class keyword, or a typedef name) means function definition; `;`, `,` or
  `=` means data definition.
- In a function definition the outermost parentheses of the declarator hold an
  identifier list (the parameters). In a declaration they must be empty. The semantic
  phase checks which applies (D29).
- A data definition with no specifiers has type `int` and storage class `extern` (A.10,
  A.13). Only `extern` and `static` are allowed at this level (A.10.2).
- The summary's `type-decl-list` and `function-statement` are replaced by
  `{ declaration } compound-statement`, as A.10.1 gives (D22).

## 4. Declarations (A.8)

```
declaration           = decl-specifiers [ init-declarator-list ] ";" ;

decl-specifiers       = ( sc-specifier | type-specifier )
                        { sc-specifier | type-specifier } ;

sc-specifier          = "auto" | "static" | "extern" | "register" | "typedef" ;

type-specifier        = "char" | "short" | "int" | "long" | "unsigned"
                      | "float" | "double"
                      | struct-or-union-specifier
                      | typedef-name ;

typedef-name          = identifier ;          (* only if declared as a typedef, section 2 *)

init-declarator-list  = init-declarator { "," init-declarator } ;

init-declarator       = declarator [ initializer ] ;
```

Notes:
- The book writes `decl-specifiers` as a right-recursive list of "type-specifier or
  sc-specifier". The `{ }` form is the same language.
- Which combinations of specifiers are valid is a semantic rule (D15, `semantics.md`
  section 3): at most one storage class, and the type-specifier combinations of D15.

### 4.1 Declarators (A.8.3, A.8.4)

The book's declarator rule is:

    declarator = identifier | "(" declarator ")" | "*" declarator
               | declarator "(" ")" | declarator "[" [ constant-expression ] "]"

Rewritten without left recursion (D14). Postfix `( )` and `[ ]` bind tighter than the
prefix `*`, as in expressions, so `*fip()` is `*(fip())`.

```
declarator            = { "*" } direct-declarator ;

direct-declarator     = ( identifier | "(" declarator ")" )
                        { "(" [ parameter-list ] ")"
                        | "[" [ constant-expression ] "]" } ;

parameter-list        = identifier { "," identifier } ;        (* function definitions only *)
```

- `"(" ")"` after a declarator means "function returning". A non-empty parameter list
  is valid only in a function definition (D29).
- `"[" "]"` with no constant expression is valid only as the first bound, or with an
  initializer (A.8.4); the semantic phase checks it.

### 4.2 Initializers (A.8.6)

The book's rule is:

    initializer      = "=" expression | "=" "{" initializer-list "}" | "=" "{" initializer-list "," "}"
    initializer-list = expression | initializer-list "," initializer-list | "{" initializer-list "}"

Rewritten without left recursion:

```
initializer           = "=" assignment-expression
                      | "=" "{" initializer-list [ "," ] "}" ;

initializer-list      = initializer-element { "," initializer-element } ;

initializer-element   = assignment-expression
                      | "{" initializer-list [ "," ] "}" ;
```

`assignment-expression` is used instead of `expression` because a comma in an
initializer list is a separator; the comma operator is allowed there only in
parentheses (A.7.15).

### 4.3 Structure and union specifiers (A.8.5)

```
struct-or-union-specifier
                      = struct-or-union [ identifier ] "{" struct-decl-list "}"
                      | struct-or-union identifier ;

struct-or-union       = "struct" | "union" ;

struct-decl-list      = struct-declaration { struct-declaration } ;

struct-declaration    = type-specifier { type-specifier } struct-declarator-list ";" ;

struct-declarator-list
                      = struct-declarator { "," struct-declarator } ;

struct-declarator     = declarator
                      | declarator ":" constant-expression       (* bit-field, stage 5 *)
                      | ":" constant-expression ;                (* unnamed field, stage 5 *)
```

- The first form, `struct { ... }`, has no tag; the second declares a tag; the third
  refers to a tag declared earlier (A.8.5).
- A structure member's declaration has no storage class.
- The book writes `type-specifier` (singular) in `struct-declaration`; the repetition
  allows `unsigned int` and `long int`.

### 4.4 Type names (A.8.7)

```
type-name             = type-specifier { type-specifier } abstract-declarator ;

abstract-declarator   = { "*" } [ direct-abstract-declarator ] ;

direct-abstract-declarator
                      = "(" abstract-declarator ")" { abstract-suffix }     (* inner part non-empty *)
                      | abstract-suffix { abstract-suffix } ;

abstract-suffix       = "(" ")" | "[" [ constant-expression ] "]" ;
```

The rule "in `( abstract-declarator )` the inner abstract-declarator must be
non-empty" (A.8.7) removes the ambiguity between `int ()` (function) and `int (x)`.

| Type name   | Meaning                                  |
|-------------|------------------------------------------|
| `int`       | integer                                  |
| `int *`     | pointer to integer                       |
| `int *[3]`  | array of 3 pointers to integers          |
| `int (*)[3]`| pointer to an array of 3 integers        |
| `int *()`   | function returning pointer to integer    |
| `int (*)()` | pointer to function returning an integer |

## 5. Statements (A.9, A.18.3)

```
compound-statement    = "{" { declaration } { statement } "}" ;

statement             = compound-statement
                      | expression ";"
                      | "if" "(" expression ")" statement
                      | "if" "(" expression ")" statement "else" statement
                      | "while" "(" expression ")" statement
                      | "do" statement "while" "(" expression ")" ";"
                      | "for" "(" [ expression ] ";" [ expression ] ";" [ expression ] ")" statement
                      | "switch" "(" expression ")" statement
                      | "case" constant-expression ":" statement
                      | "default" ":" statement
                      | "break" ";"
                      | "continue" ";"
                      | "return" [ expression ] ";"
                      | "goto" identifier ";"
                      | identifier ":" statement
                      | ";" ;
```

Notes:
- Declarations come before statements in a block (A.9.2, D32).
- The dangling `else` binds to the nearest `if` that has no `else` (A.9.3).
- The null statement `;` may carry a label (A.9.13).
- In `case`, the expression must be a constant expression (A.9.7, D28).
- The `for` statement is defined by its equivalent `while` form (A.9.6); a missing
  second expression means `while (1)`.
- `declaration-list` and `statement-list` of the book are the `{ }` repetitions.

## 6. Expressions (A.7, A.18.1)

The book's rules are ambiguous without the precedence of A.7 (D17). Below is one rule
per level, lowest precedence first. The `lvalue` category is not syntactic: the left
operand of an assignment is parsed as a unary expression and checked later.

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
equality-expression       = relational-expression
                            { ( "==" | "!=" ) relational-expression } ;
relational-expression     = shift-expression
                            { ( "<" | ">" | "<=" | ">=" ) shift-expression } ;
shift-expression          = additive-expression { ( "<<" | ">>" ) additive-expression } ;
additive-expression       = multiplicative-expression
                            { ( "+" | "-" ) multiplicative-expression } ;
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
                            | "++"
                            | "--" } ;

primary-expression        = identifier | constant | string | "(" expression ")" ;

argument-list             = assignment-expression { "," assignment-expression } ;

constant-expression       = conditional-expression ;     (* constness checked later, D28 *)
```

Notes:
- `argument-list` uses `assignment-expression`: in argument lists a comma separates
  arguments, and the comma operator appears only in parentheses. `f(a, (t=3, t+2), c)`
  has three arguments (A.7.15).
- `sizeof ( type-name )` is a unit: `sizeof (int) - 2` is `(sizeof (int)) - 2` (A.7.2).
- The third operand of `?:` is a `conditional-expression`, which gives right-to-left
  grouping (A.7.13). The middle operand is a full `expression`.
- There is no unary `+` (A.7.2).
- Postfix `++` and `--` are in `postfix-expression`, so `*p++` is `*(p++)` (D17).
- `expression-list` of the book is `argument-list`; primary-lvalue forms of the book
  (`lvalue . identifier`) are covered by `postfix-expression`.

### 6.1 Precedence and associativity

| Level | Operators                                                  | Grouping      | Book |
|-------|------------------------------------------------------------|---------------|------|
| 1     | `( )` call, `[ ]`, `.`, `->`, postfix `++` `--`            | left to right | 7.1  |
| 2     | `*` `&` `-` `!` `~` prefix `++` `--` `sizeof` cast         | right to left | 7.2  |
| 3     | `*` `/` `%`                                                | left to right | 7.3  |
| 4     | `+` `-`                                                    | left to right | 7.4  |
| 5     | `<<` `>>`                                                  | left to right | 7.5  |
| 6     | `<` `>` `<=` `>=`                                          | left to right | 7.6  |
| 7     | `==` `!=`                                                  | left to right | 7.7  |
| 8     | `&`                                                        | left to right | 7.8  |
| 9     | `^`                                                        | left to right | 7.9  |
| 10    | `|`                                                        | left to right | 7.10 |
| 11    | `&&`                                                       | left to right | 7.11 |
| 12    | `||`                                                       | left to right | 7.12 |
| 13    | `?:`                                                       | right to left | 7.13 |
| 14    | `=` `+=` `-=` `*=` `/=` `%=` `>>=` `<<=` `&=` `^=` `|=`    | right to left | 7.14 |
| 15    | `,`                                                        | left to right | 7.15 |

A.18 gives `?:` as left-to-right; A.7.13 gives right-to-left. This grammar follows
A.7.13 (D22).

## 7. Preprocessor lines (A.12, A.18.5) — stage 6

These lines are handled by the preprocessor before the lexer (D27). Their syntax is
independent of the rest of the language.

```
control-line  = "#define" identifier token-string
              | "#define" identifier "(" identifier { "," identifier } ")" token-string
              | "#undef" identifier
              | "#include" '"' filename '"'
              | "#include" "<" filename ">"
              | "#if" constant-expression
              | "#ifdef" identifier
              | "#ifndef" identifier
              | "#else"
              | "#endif"
              | "#line" constant [ identifier ] ;
```

- A control line begins with `#`. There is no space between the macro name and `(`
  in the form with arguments (A.12.1).
- A long `#define` is continued by ending the line with `\`.
- `#if` uses the constant expressions of D28.

## 8. Stage of each rule group

| Rule group                                                       | Stage |
|------------------------------------------------------------------|-------|
| `program`, `external-definition`, `function-definition`, `data-definition` | 1 |
| `declaration`, `decl-specifiers`, `sc-specifier` except `typedef` | 1     |
| `type-specifier`: `char`, `int`                                  | 1     |
| `declarator` with identifier, `( )` and parentheses              | 1     |
| `declarator` with `*` and `[ ]`                                  | 2     |
| scalar `initializer`                                             | 1     |
| brace `initializer`                                              | 2     |
| `statement`: expression, compound, `if`, `while`, `for`, `return`, `break`, `continue`, null | 1 |
| `statement`: `do`, `switch`, `case`, `default`, `goto`, labels   | 5     |
| expressions on `int` and `char`                                  | 1     |
| `[ ]`, unary `*` and `&`, strings                                | 2     |
| `.` and `->`, `struct`                                           | 3     |
| `short`, `long`, `unsigned`, `float`, `double`                   | 4     |
| `union`, `typedef`, `typedef-name`, bit-fields                   | 5     |
| `type-name` for `int` and `char`                                 | 1     |
| `type-name` with `*`, `[ ]`, `( )` suffixes                      | 2     |
| `control-line`                                                   | 6     |

A construct from a later stage produces the error "not supported yet (stage N)" from
the earliest phase that recognizes it (D2).

## 9. Differences from the book's grammar

| Topic                | Book (A.18)                                  | Here                                  | Why           |
|----------------------|----------------------------------------------|---------------------------------------|---------------|
| Lists                | right-recursive rules                        | `{ }` repetitions                     | D14           |
| Expressions          | one rule with `binop`                        | one rule per level                    | D17           |
| Declarators          | recursive, left-recursive suffixes           | prefix `*`, postfix suffixes          | D14           |
| Initializer lists    | `initializer-list , initializer-list`        | list of elements                      | D14           |
| Function definition  | `type-specifier`, `function-body`            | A.10.1 form                           | D22           |
| Data definition      | `extern` or `static` in the rule             | a declaration, checked semantically   | D22           |
| Conditional `?:`     | left to right                                | right to left                         | D22           |
| Postfix `++` `--`    | with the unary operators                     | with the postfix operators            | D17           |
| Type specifiers      | one per specifier list                       | repetition, validated semantically    | D15           |
