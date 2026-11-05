# Semantics (K&R Appendix A)

## A.4 Names: storage class and type

Every identifier is interpreted by two attributes: storage class and type.

### Storage classes
| Class     | Keyword  | Lifetime and scope (from A.4)                                  |
|-----------|----------|----------------------------------------------------------------|
| automatic | auto     | local to each invocation of a block; discarded on exit         |
| static    | static   | local to a block; keeps its value between entries              |
| external  | extern   | exists for the whole program; shared between functions and     |
|           |          | separately compiled files                                      |
| register  | register | like automatic; stored in a fast register if possible          |

`typedef` also appears in the keyword list. The book treats it in the
declarations section: record there whether it is a storage class.

### Fundamental types
| Type           | Notes                                                           |
|----------------|-----------------------------------------------------------------|
| char           | holds any member of the character set                           |
| short int      | no larger than int                                              |
| int            | natural size of the machine                                     |
| long int       | no smaller than int                                             |
| unsigned       | arithmetic modulo 2^n, n = number of bits                       |
| float          | single precision                                                |
| double         | double precision                                                |

- "Longer integers provide no less storage than shorter ones." The
  implementation may make short or long (or both) equivalent to int.
- float and double may be synonymous on some implementations.
- On the PDP-11, unsigned long is not supported. This project supports it (D6).

### Type categories (used by later rules)
- Integral: char and the integer types of all sizes (including unsigned).
- Floating: float and double.
- Arithmetic: integral and floating together.

### Derived types
Built from the fundamental types, applied recursively:
- array of T (of most types, not of functions)
- function returning T
- pointer to T
- structure containing a sequence of objects of various types
- union capable of holding one of several objects of various types

## A.5 Objects and lvalues
- An object is a manipulatable region of storage.
- An lvalue is an expression that refers to an object.
- Examples: an identifier; `*E` where E has pointer type.
- In `E1 = E2`, E1 must be an lvalue.
- Each operator in section 7 states whether it needs lvalue operands and
  whether its result is an lvalue.

## A.6 Conversions

### 6.1 Characters and integers
- A char or short may be used wherever an int may be used. The value is converted
  to int in all cases.
- Widening an integer always sign-extends, because integers are signed.
- Plain char is signed (D11): char ranges from -128 to 127. Members of the
  standard character set are non-negative.
- A character constant written with an octal escape is sign-extended:
  `'\377'` has the value -1.
- Narrowing (long to short, or any integer to char) truncates on the left: the
  excess high bits are discarded.

### 6.2 Float and double
- All floating arithmetic is done in double precision.
- A float in an expression is lengthened to double (zero-padding the fraction).
- Converting double to float (for example by assignment) rounds, then truncates
  to float length.

### 6.3 Floating and integral
- Floating to integral: the direction of truncation for negative numbers is
  machine-dependent (D16); the result is undefined if the value does not fit.
- Integral to floating: well-defined. Precision may be lost if the destination
  has too few bits.

### 6.4 Pointers and integers
- An int or long may be added to or subtracted from a pointer; the integer is
  converted as described under the addition operator (section 7).
- Two pointers to objects of the same type may be subtracted; the result is
  converted to int as described under the subtraction operator (section 7).

### 6.5 Unsigned
- Combining unsigned and plain integer: the plain integer is converted to unsigned
  and the result is unsigned. The value is the least unsigned integer congruent to
  the signed value, modulo 2^wordsize. In two's complement the bit pattern does
  not change.
- Unsigned to long: the value is preserved (zero-padding on the left).

### 6.6 Usual arithmetic conversions
Applied by most binary operators, in this order:

| Step | Condition                                  | Action                             |
|------|--------------------------------------------|------------------------------------|
| 1    | operand is char or short                   | convert to int                     |
| 1    | operand is float                           | convert to double                  |
| 2    | either operand is double                   | other becomes double; result double|
| 3    | else either operand is long                | other becomes long; result long    |
| 4    | else either operand is unsigned            | other becomes unsigned; result unsigned |
| 5    | else                                       | both are int; result int           |

Type order for the result: double > long > unsigned > int.

Worked examples:
| Expression              | Result type |
|-------------------------|-------------|
| `char + char`           | int         |
| `float * float`         | double      |
| `int + long`            | long        |
| `unsigned + int`        | unsigned    |
| `unsigned + long`       | long        |
| `short + unsigned`      | unsigned    |

Note for the type checker: steps 1 and 2 to 5 are two separate operations.
Step 1 alone is used by operators that promote one operand (for example unary
operators); steps 2 to 5 need both operand types.

## A.7 Expressions

### Evaluation (see D19, D20)
- The order of evaluation is otherwise undefined by the book. The compiler may
  compute subexpressions in any order, and side-effect order is unspecified.
- `*` `+` `&` `|` `^` may be rearranged arbitrarily, even across parentheses.
  To force an order, use an explicit temporary.
- Overflow and divide-check handling are machine-dependent.

### Primary expressions
| Form          | Requirements                    | Type of result                              | Lvalue? |
|---------------|---------------------------------|---------------------------------------------|---------|
| identifier    | declared, or implicit function (below) | its declared type                      | yes, unless array or function |
| identifier of array type | —                    | pointer to the element type                 | no      |
| identifier of function type, not in call position | — | pointer to function returning ...       | no      |
| constant      | —                               | int, long or double by form; character constants are int | no |
| string        | —                               | pointer to char (array of char, converted)  | no      |
| `( E )`       | —                               | same type as E                              | same as E |
| `E1[E2]`      | one pointer, one integer        | `*((E1)+(E2))`                              | yes     |
| `E.m`         | E an lvalue of struct or union type, m a member | type of member m                 | yes     |
| `E->m`        | E pointer to struct or union, m a member | type of member m (`(*E).m`)        | yes     |
| `F(args)`     | F of type "function returning T" | T                                         | no      |

Function calls (A.7.1):
- An unseen identifier directly followed by `(` is implicitly declared as a function
  returning int (scope: see A.13).
- Actual arguments: float becomes double, char and short become int, arrays become
  pointers. No other conversions. Argument types are not checked against formals.
- Arguments are passed by value (a copy of each). Recursion is allowed.
- Argument evaluation order is undefined in the book (D19).
- Struct and union rules are "not enforced strictly" (A.14.1).

### Unary operators
| Form       | Requirements                | Type of result                      | Lvalue? |
|------------|-----------------------------|-------------------------------------|---------|
| `*E`       | E a pointer                 | pointee type                        | yes     |
| `&L`       | L an lvalue                 | pointer to the type of L            | no      |
| `-E`       | arithmetic, usual conversions | converted type                    | no      |
| `!E`       | arithmetic or pointer       | int (1 if E is 0, else 0)           | no      |
| `~E`       | integral                    | usual-conversion type (one's complement) | no |
| `++L` `--L`| L an lvalue                 | type of L; value is the new value   | no      |
| `L++` `L--`| L an lvalue                 | type of L; value is the old value   | no      |
| `(T)E`     | T from A.8.7                | T (conversion of the value)         | not stated, treated as no |
| `sizeof E` | —                           | integer constant, size in bytes     | no      |
| `sizeof(T)`| —                           | integer constant                    | no      |

- There is no unary `+`.
- `-E` on an unsigned value is computed as 2^n minus the value, n = bits in an int.
- `++L` is equivalent to `L += 1` (see assignment).
- `sizeof` on an array gives the total number of bytes in it.

### Binary operators
| Operators        | Operands                         | Result type                      | Notes |
|------------------|----------------------------------|----------------------------------|-------|
| `*` `/`          | arithmetic, usual conversions    | converted type                   | `(a/b)*b + a%b == a` |
| `%`              | integral only                    | converted type                   | "must not be float" (read: floating types not allowed) |
| `+`              | arithmetic; or pointer and integral | converted type; or the pointer's type | integer scaled by pointee size |
| `-`              | arithmetic; pointer minus integral; pointer minus pointer (same type) | converted type; pointer type; int | pointer difference is divided by pointee size |
| `<<` `>>`        | both integral                    | type of the left operand         | usual conversions, then right operand to int |
| `<` `>` `<=` `>=`| arithmetic or pointers           | int (0 or 1)                     | pointer comparison portable only inside one array |
| `==` `!=`        | arithmetic or pointers           | int (0 or 1)                     | pointer vs integer only portable when the integer is the constant 0 |
| `&` `^` `|`      | integral only, usual conversions | converted type                   | bitwise |
| `&&` `||`        | fundamental types or pointers    | int                              | left to right, second operand skipped when the first decides the result |
| `?:`             | see below                        | see below                        | only one of the 2nd and 3rd operand is evaluated |
| `=`  `op=`       | left operand an lvalue           | type of the left operand         | value is what was stored |
| `,`              | any                              | type and value of right operand  | left value discarded |

Conditional `E1 ? E2 : E3` (A.7.13), result type, first rule that applies:
1. If the usual arithmetic conversions bring E2 and E3 to a common type, that type.
2. Else, both pointers of the same type: that type.
3. Else, one a pointer and the other the constant 0: the pointer's type.

Assignment (A.7.14):
- `E1 op= E2` is `E1 = E1 op (E2)`, with E1 evaluated once.
- Simple `=`: if both operands are arithmetic, the right is converted to the type of the left.
- For `+=` and `-=` the left operand may be a pointer, and the right is a converted
  integral value. All right operands and all non-pointer left operands must be arithmetic.
- Pointer and integer mixing, and pointer to pointer of another type: pure copy
  (D21). Assigning the constant 0 to a pointer gives a null pointer.

Shift semantics: result undefined if the right operand is negative or at least the
bit length of the left type. Left shift fills with 0. Right shift is logical for
unsigned left operands, otherwise arithmetic (D20).
