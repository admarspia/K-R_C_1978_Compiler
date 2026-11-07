# Test Vectors

Cases for every rule in `lexical.md`, `grammar.md` and `semantics.md`. They are the
source for the tests in `tests/` (C3). Each case gives input and the exact expected
outcome.

Last updated: 2026-11-06

## Notation

Tokens: `IDENT(x)`, `KW(int)`, `INT(v)` or `INT(v, long)`, `FLOAT(v)`, `CHAR(v)`,
`STR("text")`, `OP(lexeme)`, `SEP(lexeme)`, `EOF`. Every successful scan ends with `EOF`.
Errors and warnings: code and position as `line:column`. `⏎` is a newline, `→` is a tab.

## 1. Lexical vectors

### 1.1 White space, comments, positions

| Input             | Expected                                                      |
|-------------------|---------------------------------------------------------------|
| `int x;`          | KW(int) 1:1, IDENT(x) 1:5, SEP(;) 1:6, EOF                    |
| `→int`            | KW(int) 1:2                                                   |
| `a⏎b`             | IDENT(a) 1:1, IDENT(b) 2:1                                    |
| `a` `\r\n` `b`    | IDENT(a) 1:1, IDENT(b) 2:1                                    |
| `a/**/b`          | IDENT(a), IDENT(b)                                            |
| `/* c */x`        | IDENT(x) 1:8                                                  |
| `/* a⏎b */x`      | IDENT(x) 2:5                                                  |
| `/* /* */ */`     | OP(*) 1:10, OP(/) 1:11 (the first `*/` ends the comment)      |
| `/* open`         | L03 at 1:1                                                    |
| `/*/`             | L03 at 1:1                                                    |
| `a//b`            | IDENT(a), OP(/), OP(/), IDENT(b)                              |
| `a`, a form feed, `b` | L01 at the form feed; IDENT(a), IDENT(b)                   |

### 1.2 Identifiers and keywords

| Input            | Expected                                                       |
|------------------|----------------------------------------------------------------|
| `_x1`            | IDENT(_x1)                                                     |
| `x_y9`           | IDENT(x_y9)                                                    |
| `Int`            | IDENT(Int) (case-sensitive)                                    |
| `register`       | KW(register)                                                   |
| `registers`      | IDENT(registers) (D7)                                          |
| `entry`          | KW(entry)                                                      |
| `fortran asm`    | IDENT(fortran), IDENT(asm)                                     |
| `true void const`| three IDENT                                                    |
| `counter_one`    | IDENT(counter_one) (the lexer keeps the full spelling)         |
| each of the 28 keywords | KW(word)                                                |

### 1.3 Integer constants

| Input           | Expected                                         |
|-----------------|--------------------------------------------------|
| `0`             | INT(0)                                           |
| `17`            | INT(17)                                          |
| `017`           | INT(15)                                          |
| `089`           | INT(73) (digits 8 and 9 have values 8 and 9)     |
| `0x1F`          | INT(31)                                          |
| `0XfF`          | INT(255)                                         |
| `0x7FFFFFFF`    | INT(2147483647)                                  |
| `0xFFFFFFFF`    | INT(-1), type int (wraps, D6)                    |
| `2147483647`    | INT(2147483647)                                  |
| `2147483648`    | L11 at 1:1                                       |
| `0x100000000`   | L11 at 1:1                                       |
| `10L` `10l`     | INT(10, long)                                    |
| `0x1fL`         | INT(31, long)                                    |
| `0x`            | L10 at 1:1                                       |
| `12ab`          | L12 at 1:1                                       |
| `0x1G`          | L12 at 1:1                                       |
| `12ll`          | L12 at 1:1                                       |
| `00x1`          | L12 at 1:1                                       |
| `-1`            | OP(-), INT(1)                                    |

### 1.4 Floating constants

| Input        | Expected                                                |
|--------------|---------------------------------------------------------|
| `25.`        | FLOAT(25.0)                                             |
| `.5`         | FLOAT(0.5)                                              |
| `3.14`       | FLOAT(3.14)                                             |
| `1e5` `1E5`  | FLOAT(100000.0)                                         |
| `2.5e-3`     | FLOAT(0.0025)                                           |
| `1.5e+2`     | FLOAT(150.0)                                            |
| `1.e2`       | FLOAT(100.0)                                            |
| `1e`         | L09 at 1:1                                              |
| `1e+`        | L09 at 1:1                                              |
| `1.5f`       | L12 at 1:1                                              |
| `1e999`      | L13 at 1:1                                              |
| `.`          | OP(.)                                                   |
| `a.b`        | IDENT(a), OP(.), IDENT(b)                               |
| `x.5`        | IDENT(x), FLOAT(0.5)                                    |
| `1.2.3`      | FLOAT(1.2), FLOAT(0.3)                                  |
| `0x1.8`      | INT(1), FLOAT(0.8)                                      |

### 1.5 Character constants

| Input        | Expected                                                      |
|--------------|---------------------------------------------------------------|
| `'a'`        | CHAR(97)                                                      |
| `'\n'`       | CHAR(10)                                                      |
| `'\t'` `'\b'` `'\r'` `'\f'` | CHAR(9), CHAR(8), CHAR(13), CHAR(12)          |
| `'\\'`       | CHAR(92)                                                      |
| `'\''`       | CHAR(39)                                                      |
| `'"'`        | CHAR(34)                                                      |
| `'\0'`       | CHAR(0)                                                       |
| `'\101'`     | CHAR(65)                                                      |
| `'\377'`     | CHAR(-1) (sign-extended, A.6.1)                               |
| `'\400'`     | L08 at 1:2                                                    |
| `'\q'`       | CHAR(113) (backslash ignored)                                 |
| `'\8'`       | CHAR(56)                                                      |
| `'ab'`       | CHAR(24930) and W01 at 1:1 (0x6162)                           |
| `'abcd'`     | CHAR(1633837924) and W01 (0x61626364)                         |
| `'abcde'`    | L07 at 1:1                                                    |
| `''`         | L06 at 1:1                                                    |
| `'a`⏎ | L05 at 1:1                                                        |
| `'a`EOF      | L05 at 1:1                                                    |

### 1.6 Strings

| Input             | Expected                                                |
|-------------------|---------------------------------------------------------|
| `"hi"`            | STR("hi")                                               |
| `""`              | STR("")                                                 |
| `"a\"b"`          | STR(`a"b`)                                              |
| `"a'b"`           | STR(`a'b`)                                              |
| `"tab\there"`     | STR with a tab                                          |
| `"\q"`            | STR("q")                                                |
| `"a\0b"`          | STR of length 3 with an embedded NUL                    |
| `"a\⏎b"`          | STR("ab"); the next token is on line 2                  |
| `"abc⏎`           | L04 at 1:1                                              |
| `"abc`EOF         | L04 at 1:1                                              |
| `"\400"`          | L08 at 1:2                                              |
| `"a" "b"`         | STR("a"), STR("b") (not joined)                         |

### 1.7 Operators and separators

| Input        | Expected                                                      |
|--------------|---------------------------------------------------------------|
| `a+++++b`    | IDENT, OP(++), OP(++), OP(+), IDENT                           |
| `a- -b`      | IDENT, OP(-), OP(-), IDENT                                    |
| `a--b`       | IDENT, OP(--), IDENT                                          |
| `x=-1`       | IDENT, OP(=), OP(-), INT(1)                                   |
| `x>>=2`      | IDENT, OP(>>=), INT(2)                                        |
| `x >> =2`    | IDENT, OP(>>), OP(=), INT(2)                                  |
| `x<<=2`      | IDENT, OP(<<=), INT(2)                                        |
| `p->q`       | IDENT, OP(->), IDENT                                          |
| `a<b<=c`     | IDENT, OP(<), IDENT, OP(<=), IDENT                            |
| `a&&b\|\|c`  | IDENT, OP(&&), IDENT, OP(\|\|), IDENT                         |
| `a!=b`       | IDENT, OP(!=), IDENT                                          |
| `a+=1 a-=1 a*=1 a/=1 a%=1 a&=1 a^=1 a\|=1` | each compound operator is one OP |
| `( ) [ ] { } ; ,` | eight SEP tokens                                         |
| `? :`        | OP(?), OP(:)                                                  |
| `~ ! ^ %`    | four OP tokens                                                |
| `#`          | L01 at 1:1 (until the preprocessor exists, D27)               |
| `@` `$` `` ` `` `\` | L01 each                                               |
| `é`          | L01                                                           |
| `"é"`        | L02 at 1:2                                                    |
| `/* é */x`   | IDENT(x) (non-ASCII allowed in comments)                      |

### 1.8 Recovery

| Input        | Expected                                                      |
|--------------|---------------------------------------------------------------|
| `a @ b @ c`  | two L01 errors reported, tokens a, b, c still produced        |
| `1e + 2`     | L09 at 1:1, then OP(+), INT(2)                                |
| `'ab⏎x`      | L05 at 1:1, then IDENT(x) on line 2                           |
| any error    | the parser does not run (D5)                                  |

## 2. Syntax vectors (grammar and parsing)

### 2.1 Expressions

| Input                      | Parses as                                           |
|----------------------------|-----------------------------------------------------|
| `a<b == c<d`               | `(a<b) == (c<d)`                                    |
| `a<b<c`                    | `(a<b)<c`                                           |
| `a + b * c`                | `a + (b*c)`                                         |
| `a - b - c`                | `(a-b)-c`                                           |
| `a = b = c`                | `a = (b = c)`                                       |
| `a ? b : c ? d : e`        | `a ? b : (c ? d : e)` (D22)                         |
| `a, b, c`                  | comma expression, left to right                     |
| `f(a, (t=3, t+2), c)`      | call with three arguments; the second is `(t=3, t+2)` |
| `f()`                      | call with no arguments                              |
| `*p++`                     | `*(p++)`                                            |
| `-x++`                     | `-(x++)`                                            |
| `sizeof (int) - 2`         | `(sizeof (int)) - 2`                                |
| `sizeof x - 2`             | `(sizeof x) - 2`                                    |
| `(int) x + 1`              | `((int) x) + 1`                                     |
| `(x) + 1`                  | parenthesized expression plus 1 (x is not a type)   |
| `a[i][j]`                  | `(a[i])[j]`                                         |
| `p->q.r`                   | `(p->q).r`                                          |
| `x += y *= z`              | `x += (y *= z)`                                     |
| `a & b == c`               | `a & (b == c)` (equality binds tighter)             |
| `a \| b ^ c & d`           | `a \| (b ^ (c & d))`                                |
| `a && b \|\| c`            | `(a && b) \|\| c`                                   |
| `a << b + c`               | `a << (b + c)`                                      |
| `+x`                       | error: there is no unary plus                       |
| `a+++++b`                  | error: `(a++)++ + b` is not valid; `++` needs an lvalue |
| `x + = 1`                  | error (D18)                                         |

### 2.2 Declarations and type names (A.8.4, A.8.7)

| Input                                      | Meaning                                    |
|--------------------------------------------|--------------------------------------------|
| `int i, *ip, f(), *fip(), (*pfi)();`       | int; pointer to int; function returning int; function returning pointer to int; pointer to function returning int |
| `float fa[17], *afp[17];`                  | array of 17 float; array of 17 pointers to float |
| `static int x3d[3][5][7];`                 | static array of 3 arrays of 5 arrays of 7 int |
| `int *[3]` (type name)                     | array of 3 pointers to int                 |
| `int (*)[3]`                               | pointer to array of 3 int                  |
| `int *()`                                  | function returning pointer to int          |
| `int (*)()`                                | pointer to function returning int          |
| `x;` at file level                         | external int `x`                           |
| `static x;` at file level                  | static int `x`                             |
| `int f() { }`                              | accepted: empty body (D22)                 |
| `main() { }`                               | accepted: type int, storage extern         |
| `int f(a, b) int a, b; { return a+b; }`    | old-style definition (D29)                 |
| `int f(int a) { }`                         | error: prototypes are not K&R (D29)        |

### 2.3 Initializers

| Input                                                   | Expected                                |
|---------------------------------------------------------|-----------------------------------------|
| `int x[] = { 1, 3, 5 };`                                | array of 3                              |
| `float y[4][3] = { {1,3,5}, {2,4,6}, {3,5,7}, };`       | `y[3]` is 0                             |
| `float y[4][3] = { 1,3,5,2,4,6,3,5,7 };`                | same as the line above                  |
| `float y[4][3] = { {1}, {2}, {3}, {4} };`               | first column set, rest 0                |
| `char msg[] = "Syntax error on line %s\n";`             | char array from a string                |
| `int a[2] = {1,2,3};`                                   | error: more initializers than members   |
| `union u v = {1};` with `auto`                          | error: no union or automatic aggregate initializers |
| `extern int e = 1;` in a block                          | error: block-level extern cannot be initialized |

### 2.4 Statements

| Input                                              | Expected                                    |
|----------------------------------------------------|---------------------------------------------|
| `if (a) if (b) s1; else s2;`                       | the `else` belongs to `if (b)`              |
| `for (;;) s`                                       | same as `while (1) s`                       |
| `for (i = 0; i < n; i++) s`                        | `i = 0; while (i < n) { s i++; }`           |
| `{ int a; a = 1; int b; }`                         | error: declaration after a statement        |
| `L: ;`                                             | labeled null statement                      |
| `x: y;` vs `x * y;`                                | label vs expression statement               |
| `break;` outside a loop or switch                  | error                                       |
| `continue;` outside a loop                         | error                                       |
| `switch (x) { case 1: case 1: ; }`                 | error: duplicate case                       |
| `switch (x) { default: default: ; }`               | error: more than one default                |

## 3. Semantic vectors

### 3.1 Conversions (A.6.6)

| Expression                | Result type |
|---------------------------|-------------|
| `c + c` (c: char)         | int         |
| `f * f` (f: float)        | double      |
| `i + l` (int, long)       | long        |
| `u + i` (unsigned, int)   | unsigned    |
| `u + l` (unsigned, long)  | long        |
| `s + u` (short, unsigned) | unsigned    |
| `'\377'`                  | int, value -1 |
| `-u` (unsigned)           | 2^32 minus u |

### 3.2 Operators

| Case                                           | Expected                                   |
|------------------------------------------------|--------------------------------------------|
| `5 / 2`, `-5 / 2`                              | 2, -2 (toward zero, D20)                   |
| `5 % 2`, `-5 % 2`, `5 % -2`                    | 1, -1, 1 (sign of dividend)                |
| `1 << 31`                                      | -2147483648 (wraps)                        |
| `-8 >> 1`                                      | -4 (arithmetic, D20)                       |
| `u >> 1` (unsigned 0x80000000)                 | 0x40000000 (logical)                       |
| `2147483647 + 1` (variables)                   | -2147483648 (wraps)                        |
| `1 / 0` in a constant expression               | error                                      |
| `f % 2` (f: float)                             | error (D32)                                |
| `!p` (pointer)                                 | int 0 or 1                                 |
| `p + 1` (p points to int)                      | address plus 4                             |
| `p - q` (both point to int)                    | int, bytes divided by 4                    |
| `p + q`                                        | error                                      |
| `p = 0`                                        | no warning                                 |
| `p = 5`                                        | warning W03                                |
| `i = p`                                        | warning W03                                |
| `1 && 0 \|\| 1`                                | 1                                          |
| `a && f()` with `a` 0                          | `f` not called                             |
| `x ? 1 : 2.0`                                  | type double                                |
| `x ? p : 0`                                    | pointer type                               |
| `sizeof(int)`, `sizeof(char)`, `sizeof(double)`| 4, 1, 8                                    |
| `sizeof x` with `int x[10];`                   | 40                                         |
| `&r` with `register int r;`                    | error (D13)                                |
| `&3`                                           | error: not an lvalue                       |
| `3 = x`                                        | error: not an lvalue                       |
| `x++ = 1`                                      | error: not an lvalue                       |
| `a[1]` and `1[a]`                              | same value                                 |

### 3.3 Declarations and scope

| Case                                                          | Expected                              |
|---------------------------------------------------------------|---------------------------------------|
| `unsigned long x;`                                            | error (D15)                           |
| `unsigned char c;`                                            | error (D15)                           |
| `long float f;`                                               | double                                |
| `short int s;`                                                | short                                 |
| `int int x;`                                                  | error                                 |
| `auto int a; static int a;` in one block                      | error: redeclaration                  |
| `int x; { int x; }`                                           | inner `x` hides outer                 |
| `extern int e;` with no definition anywhere                   | error at link time                    |
| `f();` with `f` undeclared                                    | implicit `extern int f()`             |
| `int f(); ... double f();` in one file                        | error: incompatible redeclaration     |
| `struct s { struct s x; };`                                   | error: contains itself                |
| `struct s { struct s *x; };`                                  | accepted                              |
| `struct a { int n; }; struct b { char n; };`                  | error: member `n` with different types (D24) |
| `struct t { int x; }; int t;`                                 | accepted: tag and variable are in different classes |
| `struct t { int t; };`                                        | error: tag and member names must be distinct |
| `a = s1;` with structures                                     | error (D26)                           |
| `f(s1)` with a structure argument                             | error (D26)                           |
| identifiers `counter_one`, `counter_two` in one scope         | W02 and redeclaration error (D10)     |
| `typedef float distance; { auto int distance; }`              | `distance` is an int variable in the block |
| `typedef float distance; { auto distance; }`                  | declaration with no declarators       |

### 3.4 Constant expressions (D28)

| Use                          | Input                      | Expected |
|------------------------------|----------------------------|----------|
| `case`                       | `case 'a' + 1:`            | accepted |
| `case`                       | `case sizeof(int) * 2:`    | accepted |
| array bound                  | `int a[2 * 3];`            | accepted |
| array bound                  | `int a[0];`                | error (D32) |
| array bound                  | `int a[n];` (n variable)   | error    |
| `case`                       | `case f(1):`               | error    |
| `case`                       | `case 1.5:`                | error    |
| `case`                       | `case a && b:`             | error    |
| static initializer           | `static int *p = &x;` with static `x` | accepted |
| static initializer           | `static int y = z;` (z variable) | error |
| automatic initializer        | `int y = z + 1;`           | accepted |

## 4. Program vectors (from the book's Appendix A)

| Source                                                                    | Stage |
|---------------------------------------------------------------------------|-------|
| the `max(a, b, c)` function of A.10.1                                     | 1     |
| the `tnode` structure and the `sp->count`, `s.left`, `s.right->tword[0]` expressions of A.8.5 | 3 |
| the `typedef` examples of A.8.8 (`MILES`, `KLICKSP`, `complex`)           | 5     |
| the `alloc` example of A.14.4: `extern char *alloc(); double *dp; dp = (double *) alloc(sizeof(double)); *dp = 22.0 / 7.0;` | 4 |
| `int x3d[3][5]` subscripting of A.14.3                                    | 2     |

Programs from the main text of the book are copied into `examples/`, each tagged with
the stage it needs.
