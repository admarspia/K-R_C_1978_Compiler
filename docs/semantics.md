# Semantics (K&R Appendix A, sections 4 to 17)

The meaning of programs and the checks the semantic phase makes. Sections follow the
book. Every choice the book leaves open is a decision (D-numbers); every restriction
beyond the book is marked.

Last updated: 2026-11-06

## 1. Storage classes (A.4, A.8.1)

Every identifier has a storage class and a type.

| Class     | Keyword    | Lifetime and scope                                                    |
|-----------|------------|-----------------------------------------------------------------------|
| automatic | `auto`     | local to each invocation of a block; discarded on exit               |
| static    | `static`   | local to a block; keeps its value between entries; initialized once at program start |
| external  | `extern`   | exists for the whole program; shared between functions and separately compiled files |
| register  | `register` | like automatic; stored in a fast register if possible (D13)          |
| (typedef) | `typedef`  | does not reserve storage; "storage class" only for syntactic convenience (A.8.1) |

Where each may appear:

| Place                       | Allowed                                  | Default            |
|-----------------------------|------------------------------------------|--------------------|
| external definition (data)  | `extern`, `static`                       | `extern`           |
| external definition (function) | `extern`, `static`                    | `extern`           |
| block declaration           | `auto`, `static`, `extern`, `register`, `typedef` | `auto`    |
| block declaration of a function | `extern` (functions are never automatic) | `extern`       |
| function parameter          | `register` only                          | automatic          |
| structure member            | none                                     | from context       |

- At most one storage class per declaration (A.8.1).
- `auto`, `static` and `register` declarations also define the object (they reserve
  storage). A block-level `extern` does not, so it cannot have an initializer (A.9.2).
- An `extern` declaration requires an external definition of the identifier somewhere
  outside the function (A.8.1, A.11.2).

## 2. Types (A.4)

### 2.1 Fundamental types

| Type       | Notes (model of D6)                                                        |
|------------|----------------------------------------------------------------------------|
| `char`     | 8 bits, signed, -128 to 127; holds any member of the character set (D11)  |
| `short`    | 16 bits                                                                    |
| `int`      | 32 bits, the natural size                                                  |
| `long`     | 32 bits; no smaller than int                                               |
| `unsigned` | 32 bits; arithmetic modulo 2^32                                            |
| `float`    | single precision                                                           |
| `double`   | double precision                                                           |

short, int and long are distinct types even when equal in width (D12).

Type categories used by later rules:

| Category   | Members                                       |
|------------|-----------------------------------------------|
| integral   | `char` and all sizes of `int`, `unsigned`     |
| floating   | `float`, `double`                             |
| arithmetic | integral and floating                         |
| scalar     | arithmetic and pointers                       |
| aggregate  | arrays and structures                         |

### 2.2 Derived types

Built from the fundamental types and from each other, recursively (A.4):

- array of T (of most types)
- function returning T
- pointer to T
- structure containing a sequence of named members of various types
- union capable of holding any one of several members

Restrictions (A.8.4):
- A function may not return an array, a structure, a union or a function (it may
  return a pointer to such things).
- There are no arrays of functions (there may be arrays of pointers to functions).
- A structure or union may not contain a function (it may contain a pointer to one).
- A structure or union may not contain an instance of itself, only a pointer to one
  (A.8.5).

### 2.3 Type-specifier combinations (A.8.2, D15)

Valid specifier sets (any order of words within a set is not specified; use the order shown):

| Written                          | Type                     |
|----------------------------------|--------------------------|
| `char`                           | char                     |
| `short`, `short int`             | short                    |
| `int`                            | int                      |
| `long`, `long int`               | long                     |
| `unsigned`, `unsigned int`       | unsigned                 |
| `float`                          | float                    |
| `double`, `long float`           | double                   |
| struct-or-union specifier        | that structure or union  |
| typedef name                     | the named type           |
| (none)                           | int                      |

Everything else is an error: `unsigned char`, `unsigned short`, `unsigned long`,
`long double`, `int int`, `short long`, and so on. There is no `void`, `signed`,
`const`, `volatile` or `enum`.

## 3. Objects and lvalues (A.5)

- An object is a manipulatable region of storage. An lvalue is an expression that refers
  to an object.
- Identifiers of arithmetic, pointer, structure or union type are lvalues.
- An array identifier is not an lvalue (A.7.1); a function name is not an lvalue.
- `*E` (E a pointer) is an lvalue. `E1[E2]` is an lvalue. `E.m` is an lvalue if E is.
  `E->m` is an lvalue. A parenthesized lvalue is an lvalue.
- The left operand of every assignment operator, and the operand of `++`, `--` and
  unary `&`, must be an lvalue.
- The results of prefix and postfix `++` and `--`, of calls, casts, arithmetic and
  `&` are not lvalues.
- Unary `&` cannot be applied to a register variable (D13) or to a bit-field (D30).

## 4. Conversions (A.6)

### 4.1 Characters and integers (A.6.1)
- A char or short may be used wherever an int may be used; the value is converted to int.
- Widening a signed integer sign-extends.
- Plain char is signed (D11); `'\377'` is -1.
- Narrowing (a longer integer to a shorter one or to char) truncates on the left: the
  excess high bits are discarded.

### 4.2 Float and double (A.6.2)
- All floating arithmetic is done in double precision.
- A float in an expression is lengthened to double.
- Converting double to float (for example by assignment) rounds, then truncates to
  float length.

### 4.3 Floating and integral (A.6.3)
- Floating to integral truncates toward zero (D16); if the value does not fit, the
  result is undefined.
- Integral to floating is well behaved; precision may be lost.

### 4.4 Pointers and integers (A.6.4, A.14.4)
- An integer may be added to or subtracted from a pointer (A.7.4).
- Two pointers to the same type may be subtracted; the result is an int.
- A pointer may be converted by a cast to an integral type large enough to hold it
  (int or long under D6), and an integral value to a pointer. Round trips are exact.
- A pointer to one type may be converted by a cast to a pointer to another type. A
  pointer to an object of a given size may be converted to a pointer to a smaller
  object and back without change.
- Mixed assignments produce warning W03 (D21).

### 4.5 Unsigned (A.6.5)
- Unsigned combined with a plain integer: the integer becomes unsigned, and the result
  is unsigned (value modulo 2^32, no change in bit pattern).
- Unsigned to long: the value is preserved.

### 4.6 The usual arithmetic conversions (A.6.6)

Step 1 is the promotion (also used alone by some unary operators). Steps 2 to 5 apply
to two operands.

| Step | Condition                       | Action                                    |
|------|---------------------------------|-------------------------------------------|
| 1    | operand is char or short        | convert to int                            |
| 1    | operand is float                | convert to double                         |
| 2    | either operand is double        | other becomes double; result is double    |
| 3    | else either operand is long     | other becomes long; result is long        |
| 4    | else either operand is unsigned | other becomes unsigned; result is unsigned|
| 5    | else                            | both are int; result is int               |

| Expression              | Result |
|-------------------------|--------|
| `char + char`           | int    |
| `float * float`         | double |
| `int + long`            | long   |
| `unsigned + int`        | unsigned |
| `unsigned + long`       | long (step 3 comes before step 4) |
| `short + unsigned`      | unsigned |

## 5. Expressions (A.7)

### 5.1 General (A.7)
- Precedence follows the order of the subsections (grammar section 6.1).
- Order of evaluation is otherwise undefined by the book; D19 fixes it left to right.
- `*` `+` `&` `|` `^` may be rearranged by the compiler, even across parentheses; use
  an explicit temporary to force an order. This project does not rearrange (D19).
- Overflow and divide-check handling are machine-dependent (D20).

### 5.2 Primary expressions (A.7.1)

| Form                        | Requirements                                  | Type of result                                 | Lvalue |
|-----------------------------|-----------------------------------------------|------------------------------------------------|--------|
| identifier                  | declared, or implicitly a function (D25)      | declared type                                  | yes, except array and function |
| identifier of type "array of T" | —                                         | pointer to T (the value is the first element's address) | no |
| identifier of type "function returning T", not called | —                   | pointer to function returning T                | no     |
| constant                    | —                                             | int, long or double by form; character constants are int | no |
| string                      | —                                             | pointer to char (type array of char, converted) | no    |
| `( E )`                     | —                                             | type of E                                      | as E   |
| `E1[E2]`                    | one pointer or array, one int                 | pointee type; identical to `*((E1)+(E2))`      | yes    |
| `F(args)`                   | F of type "function returning T"              | T                                              | no     |
| `E.m`                       | E an lvalue of struct or union type, m a member | type of m                                    | yes    |
| `E->m`                      | E a pointer to struct or union, m a member    | type of m; same as `(*E).m`                    | yes    |

Function calls (A.7.1):
- A not-yet-declared identifier followed by `(` is implicitly declared as a function
  returning int (D25).
- Actual arguments: `float` becomes `double`; `char` and `short` become `int`; array
  names become pointers. No other conversions. The compiler does not compare argument
  types with the formal parameters; use a cast if needed.
- All arguments are passed by value: a copy is made of each. A function may change its
  formal parameters without affecting the actual ones.
- Recursion is allowed. Argument order is left to right (D19).
- Subscript: `E1[E2]` is `*((E1)+(E2))`, so `1[a]` is valid (A.14.3).

### 5.3 Unary operators (A.7.2)

| Form        | Requirements                  | Type of result                          | Lvalue |
|-------------|-------------------------------|-----------------------------------------|--------|
| `*E`        | E a pointer                   | the pointee type                        | yes    |
| `&L`        | L an lvalue                   | pointer to the type of L                | no     |
| `-E`        | arithmetic; usual conversions | converted type                          | no     |
| `!E`        | arithmetic or pointer         | int: 1 if E is 0, else 0                | no     |
| `~E`        | integral; usual conversions   | converted type (one's complement)       | no     |
| `++L` `--L` | L an lvalue                   | type of L; value is the new value       | no     |
| `L++` `L--` | L an lvalue                   | type of L; value is the old value       | no     |
| `(T)E`      | T from A.8.7                  | T                                       | no     |
| `sizeof E`  | —                             | int constant, size in bytes             | no     |
| `sizeof(T)` | —                             | int constant                            | no     |

- There is no unary `+`.
- The negative of an unsigned value is 2^n minus the value, n the number of bits in an int.
- `++x` is equivalent to `x += 1` (conversions: A.7.4, A.7.14). `--x` is analogous.
- `sizeof` on an array gives the total bytes in it. A byte is the space for a `char`.
  `sizeof` is "semantically an integer constant" and may appear wherever a constant is
  required. `sizeof (T) - 2` is `(sizeof (T)) - 2`.

### 5.4 Binary operators (A.7.3 to A.7.15)

| Operators         | Operand requirements                              | Result type                           |
|-------------------|---------------------------------------------------|---------------------------------------|
| `*` `/`           | arithmetic; usual conversions                     | converted type                        |
| `%`               | integral (D32); usual conversions                 | converted type                        |
| `+`               | arithmetic; or pointer and integral               | converted type; or the pointer's type |
| `-`               | arithmetic; pointer minus integral; pointer minus pointer (same type) | converted type; pointer type; int |
| `<<` `>>`         | both integral; usual conversions, then right operand to int | type of the left operand    |
| `<` `>` `<=` `>=` | arithmetic or pointers; usual conversions         | int (0 or 1)                          |
| `==` `!=`         | arithmetic or pointers; a pointer may be compared with an integer | int (0 or 1)          |
| `&` `^` `\|`      | integral; usual conversions                       | converted type                        |
| `&&` `\|\|`       | each operand fundamental type or pointer          | int                                   |
| `?:`              | see below                                         | see below                             |
| `=` and `op=`     | left operand an lvalue                            | type of the left operand              |
| `,`               | any                                               | type and value of the right operand   |

Details:
- `/`: with positive integers truncation is toward 0; with a negative operand it is
  machine-dependent (D20: toward 0). `(a/b)*b + a%b` equals `a` when `b` is not 0.
  The remainder has the sign of the dividend.
- `+`: an integral value added to a pointer is multiplied by the pointee size, so
  `p+1` points to the next object in the array. No other pointer combinations are allowed.
- `-` on two pointers to the same type divides the byte difference by the object size and
  gives an int. Results are meaningful only inside one array.
- Shifts: the result is undefined if the right operand is negative or at least the width
  in bits. Left shift fills with 0. Right shift is logical for unsigned, arithmetic otherwise (D20).
- Relational operators group left to right, which is not useful: `a<b<c` is `(a<b)<c`.
  Pointer comparison is portable only inside one array.
- `==` and `!=` are like the relational operators with lower precedence; `a<b == c<d` is
  1 whenever `a<b` and `c<d` have the same truth value. A pointer compared with an integer
  is portable only when the integer is the constant 0.
- `&&` and `||` group left to right, evaluate left to right, and do not evaluate the second
  operand when the first decides the result. The operands may have different types.
- `&`, `^`, `|`: associative; expressions may be rearranged.

**Conditional `E1 ? E2 : E3`** (A.7.13). E1 is evaluated; if non-zero the result is E2,
otherwise E3. Only one of E2 and E3 is evaluated. The result type is, in order:
1. the common type, if the usual arithmetic conversions can bring E2 and E3 to one;
2. else, if both are pointers of the same type, that type;
3. else, one must be a pointer and the other the constant 0, and the type is the pointer.

**Assignment** (A.7.14):
- All assignment operators group right to left. The value is the value stored in the left
  operand after the assignment.
- Simple `=`: if both operands are arithmetic, the right operand is converted to the type of
  the left before assignment.
- `E1 op= E2` is `E1 = E1 op (E2)` with `E1` evaluated once. For `+=` and `-=` the left
  operand may be a pointer and the right is integral (converted as in A.7.4); otherwise
  all right operands and all left operands must be arithmetic.
- Pointer to integer, integer to pointer and pointer to a pointer of another type are pure
  copies, with warning W03 (D21). Assigning the constant 0 to a pointer gives a null
  pointer, distinguishable from a pointer to any object.
- Structures and unions cannot be assigned (D26).

**Comma** (A.7.15): evaluates left to right and discards the left value. In argument lists
and initializer lists a comma is a separator; the comma operator must be in parentheses
there.

## 6. Declarations (A.8)

### 6.1 Meaning of declarators (A.8.4)

A declarator is an assertion: if a construction of the same form appears in an
expression, it yields an object of the stated type. Each declarator contains exactly one
identifier. For a declaration `T D1`, where the identifier has type "... T":

| D1 has the form  | The identifier has type          |
|------------------|----------------------------------|
| plain identifier | T                                |
| `*D`             | ... pointer to T                 |
| `D()`            | ... function returning T         |
| `D[const-expr]`  | ... array of T                   |
| `D[]`            | ... array of T (size unspecified)|
| `(D)`            | same as D; parentheses change binding |

Array rules (A.8.4):
- The constant expression is an int constant expression (D28, D32).
- With several adjacent array specifications the array is multi-dimensional. The bounds
  may be missing only for the first. This is useful for an external array whose definition
  is elsewhere.
- The first bound may also be omitted if the declarator has an initializer; the size is
  then the number of initial elements.
- An array may be built from a basic type, a pointer, a structure or union, or another array.

### 6.2 Examples from the book (use as tests)

| Declaration                         | Meaning                                                    |
|-------------------------------------|------------------------------------------------------------|
| `int i, *ip, f(), *fip(), (*pfi)();`| i: int; ip: pointer to int; f: function returning int; fip: function returning pointer to int; pfi: pointer to function returning int |
| `float fa[17], *afp[17];`           | fa: array of 17 float; afp: array of 17 pointers to float  |
| `static int x3d[3][5][7];`          | static array of 3 arrays of 5 arrays of 7 int; `x3d`, `x3d[i]`, `x3d[i][j]` have array type, `x3d[i][j][k]` has type int |

### 6.3 Structure and union declarations (A.8.5) — stage 3 and 5

- A structure is a sequence of named members, each of any type. A union holds one of
  several members at a time, all starting at offset 0; its size holds the largest member.
- Member addresses increase in declaration order. Each non-field member starts at an
  alignment boundary for its type (D6), so there may be unnamed holes.
- A tag declared with `struct tag { ... }` can later be used as `struct tag`. Tags allow
  self-referential structures (a member that points to the same structure).
- Member and tag names may equal ordinary variable names. Tags and members must be
  mutually distinct (D23, D24).
- Two structures may share a common initial sequence of members: the same name may be a
  member of two structures if it has the same type and offset in both (D24).
- Structure declaration example (A.8.5):

      struct tnode { char tword[20]; int count; struct tnode *left; struct tnode *right; };
      struct tnode s, *sp;

  `sp->count` is the `count` of the structure `sp` points to, `s.left` is the left pointer
  of `s`, and `s.right->tword[0]` is the first character of the right subtree's word.
- Bit-fields (D30): `declarator : constant-expression` and `: constant-expression`.
- Operations (D26, A.14.1): a structure or union may only have a member named with `.` or
  its address taken with `&`. Assignment to or from it, and passing it as a parameter,
  are errors.

### 6.4 Initialization (A.8.6)

- An initializer is `=` followed by an expression or a brace-enclosed list.
- Static and external objects: every expression must be a constant expression, or reduce
  to the address of a previously declared external or static object, possibly offset by a
  constant (D28). Automatic and register scalars may use arbitrary expressions involving
  constants, previously declared variables and functions.
- Uninitialized static and external objects start as 0. Uninitialized automatic and
  register objects start as garbage.
- A scalar initializer is a single expression, perhaps in braces. The value is converted as
  for assignment.
- An aggregate (structure or array) initializer is a brace-enclosed, comma-separated list for
  the members in increasing subscript or member order. If the aggregate has sub-aggregates
  the rule applies recursively. With fewer initializers than members, the rest are 0.
- It is not permitted to initialize unions or automatic aggregates.
- Brace elision: if an initializer begins with `{`, the list initializes the members of the
  aggregate, and more initializers than members is an error. If it does not begin with `{`,
  only enough elements are taken to fill the members of the aggregate, and the rest go to
  the next member of the enclosing aggregate.
- A char array may be initialized by a string.
- Block rules (A.9.2): initialization of `auto` and `register` objects runs each time the
  block is entered at the top (not when entered by `goto` or by a `switch` case); `static`
  objects are initialized once at program start; `extern` in a block cannot be initialized.

Examples from the book (use as tests):

| Declaration                                      | Result                                       |
|--------------------------------------------------|----------------------------------------------|
| `int x[] = { 1, 3, 5 };`                         | array of 3 int                               |
| `float y[4][3] = { {1,3,5}, {2,4,6}, {3,5,7}, };`| rows 0 to 2 set, `y[3]` is 0                 |
| `float y[4][3] = { 1,3,5,2,4,6,3,5,7 };`         | same result by brace elision                 |
| `float y[4][3] = { {1}, {2}, {3}, {4} };`        | first column set, rest 0                     |
| `char msg[] = "Syntax error on line %s\n";`      | char array initialized from the string       |

### 6.5 Type names (A.8.7)

A type name is a declaration with the identifier omitted. It is used in a cast and in
`sizeof`. The grammar's table in `grammar.md` section 4.4 lists the meanings.

### 6.6 Typedef (A.8.8) — stage 5

- A declaration whose storage class is `typedef` defines no storage. Each identifier in its
  declarators becomes a name syntactically equivalent to the type keyword for the type the
  declarator gives (A.8.4).
- A typedef introduces a synonym, not a new type.
- Example: after `typedef int MILES, *KLICKSP; typedef struct { double re, im; } complex;`
  `MILES distance;` has type int, `extern KLICKSP metricp;` has type pointer to int, and
  `complex z, *zp;` give a structure and a pointer to it.
- Scope: typedef names are ordinary identifiers (D23) and may be redeclared in an inner
  block, but an explicit type must then be given: after `typedef float distance;`, the
  inner declaration must be `auto int distance;`, because `auto distance;` would declare no
  variable (grammar section 2).

## 7. Statements (A.9)

| Statement | Meaning and checks                                                                 |
|-----------|-------------------------------------------------------------------------------------|
| `E;`      | evaluates E; usually an assignment or a call                                        |
| `{ ... }` | compound statement; declarations first; a declaration shadows an outer one until the block ends |
| `if (E) S`, `if (E) S else S` | E non-zero runs the first; the second runs when E is 0; `else` binds to the nearest else-less `if` |
| `while (E) S` | repeats S while E is non-zero; test before each execution                      |
| `do S while (E);` | repeats until E is zero; test after each execution (stage 5)                |
| `for (E1; E2; E3) S` | same as `E1; while (E2) { S E3; }`; any of the three may be omitted; missing E2 means `while (1)`; missing others are dropped |
| `switch (E) S` | E is converted and must be int (D32); S is typically compound (stage 5)       |
| `case C: S` | C an int constant expression; no two equal in one switch                          |
| `default: S` | at most one per switch                                                           |
| `break;`  | ends the smallest enclosing `while`, `do`, `for` or `switch`; error outside them    |
| `continue;` | jumps to the end of the smallest enclosing `while`, `do` or `for` body (as if `goto contin;` placed before the closing brace); error outside a loop |
| `return;` | returns with an undefined value                                                     |
| `return E;` | returns E, converted as by assignment to the function's type                      |
| `goto L;` | transfers to label L, which must be in the current function (stage 5)               |
| `L: S`    | declares L as a label; scope is the whole function, excluding sub-blocks where the same identifier is redeclared |
| `;`       | null statement; may carry a label or be a loop body                                 |

Additional rules:
- A switch: `case` and `default` prefixes do not alter the flow of control, which
  continues across them. With no matching case and no `default`, nothing in the switch runs.
  Declarations may appear at the head of the switch body, but initializations of `auto` and
  `register` variables are ineffective there.
- Flowing off the end of a function is the same as `return;` with no value.
- It is possible (but a bad practice) to jump into a block; initializations are then skipped.

## 8. External definitions (A.10)

- A program is a sequence of external definitions. Only at this level may function code be given.
- **Function definition** (D29): `[specifiers] name(param, ...) declarations { body }`.
  - Allowed storage classes: `extern` (default) or `static`.
  - A missing type-specifier means int.
  - Only the identifiers in the parameter list may be declared in the declaration list;
    undeclared parameters are int. The only storage class allowed there is `register`.
  - Declared `float` parameters become `double`; "array of ..." parameters become
    "pointer to ..."; declaring a structure, union or function parameter is useless, because
    they cannot be passed (pointers to them can).
  - Example (use as a test):

        int max(a, b, c)
        int a, b, c;
        {
            int m;
            m = (a > b) ? a : b;
            return((m > c) ? m : c);
        }

- **Data definition** (A.10.2): a declaration at the top level with storage class `extern`
  (default) or `static`, never `auto` or `register`.
- The scope of an external definition lasts to the end of the file (A.10, A.11.1).

## 9. Scope rules (A.11)

### 9.1 Lexical scope (A.11.1)

| Identifier                    | Scope                                                       |
|-------------------------------|-------------------------------------------------------------|
| declared in an external definition | from the definition to the end of the source file      |
| formal parameter              | the whole function                                          |
| declared at the head of a block | until the end of the block                                |
| label                         | the whole function                                          |

- An explicit declaration of an identifier at the head of a block, including the function's
  own block, suspends any declaration of it outside the block until the end of the block.
- There are two name classes, which never conflict (D23): ordinary identifiers (with
  typedef names and labels), and members and tags.
- All declarations of the same external identifier are checked for compatibility; their
  effective scope grows to the whole file.

### 9.2 Scope of externals (A.11.2)

- If a function refers to an `extern` identifier, an external definition for it must exist
  somewhere in the program.
- All references to the same external identifier refer to the same object; type and size
  in the definition must be compatible with those in each referencing function.
- The `extern` keyword in an external definition says the storage is allocated elsewhere.
  In a multi-file program, an external data definition without `extern` must appear in exactly
  one file. Only the declaration that allocates storage may initialize.
- Identifiers declared `static` at the top level are not visible in other files. Functions may
  be `static`.
- The external-name length limits of A.2.2 are not enforced (D10).

## 10. Implicit declarations (A.13, D25)

| Situation                                                     | Result                                  |
|---------------------------------------------------------------|-----------------------------------------|
| storage class given, no type                                  | type is `int`                           |
| type given, no storage class, inside a function               | `auto`                                  |
| type "function returning ...", no storage class               | `extern` (functions are never automatic)|
| identifier followed by `(`, not already declared, in an expression | `extern` function returning `int`, entered at file scope |
| storage class in external definitions, parameters, members    | supplied by context                     |

## 11. Types revisited (A.14)

- **Structures and unions** (A.14.1, D26): only member naming with `.` and `&`.
- **Functions** (A.14.2): only calling and taking the address. A function name not in call
  position becomes a pointer to the function. Passing a function name to another function
  requires an explicit declaration of the function (`int f();`) in the caller, because its
  appearance was not followed by `(`.
- **Arrays, pointers and subscripting** (A.14.3):
  - An array identifier in an expression becomes a pointer to its first member, so arrays
    are not lvalues.
  - `E1[E2]` is `*((E1)+(E2))`; subscripting is commutative.
  - For an n-dimensional array of rank `i x j x ... x k`, the array name in an expression
    becomes a pointer to an (n-1)-dimensional array of rank `j x ... x k`. Applying `*` (or
    subscripting) gives that array, which converts to a pointer again.
  - Arrays are stored row-wise (last subscript varies fastest). The first subscript in a
    declaration affects storage size but plays no other part in subscript calculations.
  - Example: for `int x[3][5];` the expression `x[i]` is `*(x+i)`; `i` is scaled by 5 integers.
- **Explicit pointer conversions** (A.14.4): see section 4.4. Alignment: a pointer converted
  to a more strictly aligned type may cause an addressing fault; D6 fixes alignment equal to size.

## 12. Constant expressions (A.15, D28)

| Use                         | Allowed                                                      |
|-----------------------------|--------------------------------------------------------------|
| `case`, array bounds, `#if` | integer, character and `sizeof` constants joined by `+ - * / % & \| ^ << >> == != < > <= >=`, unary `-` and `~`, and `?:`; parentheses for grouping; no function calls, no `&&` `\|\|` `!` |
| initializers (static, external) | the above, or the address of a previously declared external or static object plus or minus a constant (see D28) |
| initializers (auto, register scalars) | any expression                                  |

## 13. Portability decisions (A.16)

The book lists machine-dependent areas. The choices made here:

| Area                           | Choice                              | Decision |
|--------------------------------|-------------------------------------|----------|
| word sizes and float format    | D6 table                            | D6       |
| sign extension of char         | signed, sign-extends                | D11      |
| byte order, bit-field order    | left to right                       | D6, D30  |
| argument evaluation order      | left to right                       | D19      |
| side-effect order              | otherwise unspecified               | D19      |
| multi-character constants      | permitted, first character highest  | D8       |
| `register` count and types     | hint; excess ignored                | D13      |
| overflow, divide by zero       | wrap; unchecked                     | D20      |

## 14. Anachronisms (A.17, D31)

The `=op` assignment forms and initializers without `=` are not supported.

## 15. Diagnostics added by the semantic phase

| Code | Kind    | Condition                                                                |
|------|---------|--------------------------------------------------------------------------|
| W02  | warning | two identifiers differ only after the eighth character (D10)             |
| W03  | warning | nonportable pointer and integer or pointer-type conversion (D21)         |

Error messages for semantic errors are fixed text with line and column, like lexical errors
(C5). They are added to this table as each rule is implemented; numbers are never reused.
