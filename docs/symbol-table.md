# Symbol Table Design

Design only: what the table must hold and answer, from the language definition.
No code. The package is `src/symbol`. Rules cited here are defined in `semantics.md`
and `decisions.md`.

Last updated: 2026-11-06

## 1. What the table is for

| Phase               | Needs from the table                                                            |
|---------------------|---------------------------------------------------------------------------------|
| parser (stage 5)    | "is this identifier a typedef name?" during parsing (grammar section 2)         |
| semantic analysis   | type of every name, redeclaration and shadowing rules, implicit declarations, member lookup, storage class checks |
| code generation     | storage class, size, alignment, offsets, linkage (external or static)           |

Because the parser needs typedef lookup, the table exists from the start of parsing and
is updated as declarations are parsed, not only after the whole tree is built.

## 2. Name classes (D23, D24)

Two classes, kept apart:

| Class | Holds                                                                    |
|-------|--------------------------------------------------------------------------|
| 1     | ordinary identifiers: variables, functions, parameters, typedef names, labels |
| 2     | structure and union members and tags (one shared class)                  |

A lookup names the class it searches. Class 2 has one more rule: a member name may
appear in two structures only if the type and offset agree (D24), so class 2 entries are
global per name, not per structure. Each structure keeps its own ordered member list
that refers to those entries.

## 3. Scopes (A.11)

| Scope kind  | Created by                          | Ends at                          |
|-------------|-------------------------------------|----------------------------------|
| file        | the start of the translation unit   | end of the source file           |
| function    | a function definition (parameters, labels) | end of the function       |
| block       | a compound statement                | its closing brace                |

- The scopes form a stack. Entering a block pushes a scope; leaving pops it.
- A function's parameters are in the function scope, and the function's outermost block
  is a block scope inside it. A declaration at the head of the function's block hides a
  parameter of the same name (A.11.1: "including the block constituting a function").
- Labels belong to the function scope and are visible in the whole function, including
  inside nested blocks, unless an inner block redeclares the identifier (A.9.12, D23).
  Labels can be used before they are declared (forward `goto`), so their resolution is
  done at the end of the function.
- Lookup searches from the innermost scope outward. A declaration in an inner scope
  hides, but does not remove, outer ones.
- Redeclaration in the same scope and class is an error, except where the book allows
  compatible repeated declarations: external declarations of the same identifier must
  be compatible (A.11.1, A.11.2), and an `extern` block declaration may repeat an
  external one.

## 4. Entry kinds and fields

Every entry has: name (full spelling), kind, scope, declaration position (line and column),
class, and the fields below.

| Kind       | Class | Fields                                                                  |
|------------|-------|-------------------------------------------------------------------------|
| variable   | 1     | type, storage class (auto, static, extern, register), has-initializer, later: stack offset or symbol name, size, alignment |
| function   | 1     | return type, storage class (extern or static), defined or only referenced, implicit flag, parameter names (for the definition) |
| parameter  | 1     | type, register flag, position                                           |
| typedef name | 1   | the type it names                                                       |
| label      | 1     | defined flag, referenced flag, position                                 |
| member     | 2     | type, offset, bit-field width and position (stage 5), owning structure(s) |
| tag        | 2     | kind (struct or union), member list (ordered), complete or incomplete, size, alignment |

Common derived queries: "is declared", "is a typedef name", "is a function", "is an lvalue
candidate" (variable, parameter, or member).

## 5. Identifier significance (D10)

- Entries are keyed by the first 8 characters of the identifier, per class and scope.
- The entry keeps the full spelling for messages.
- When a lookup or insertion finds an entry with the same key but a different full
  spelling in the same scope, warning W02 names both spellings; an insertion is also a
  redeclaration error.
- The limit is one named constant.

## 6. Type representation (D12)

The table stores types, so the type model is part of its design. A type is a tree:

| Type form             | Parts                                                    |
|-----------------------|----------------------------------------------------------|
| basic                 | one of: char, short, int, long, unsigned, float, double  |
| pointer               | pointee type                                             |
| array                 | element type, length (or unknown for `[]`)               |
| function              | return type (no parameter types: A.8.3, A.7.1)           |
| structure or union    | reference to its tag entry (class 2)                     |

Operations the type model must answer (used by semantic analysis and code generation):

| Query                          | Used for                                       |
|--------------------------------|------------------------------------------------|
| size and alignment             | `sizeof`, offsets, pointer arithmetic (D6)     |
| is integral, floating, arithmetic, scalar, aggregate | the operand rules of `semantics.md` |
| are two types identical / compatible | redeclarations, member checks (D24), `?:` |
| the type after the usual arithmetic conversions | binary operators, A.6.6        |
| the type after array and function adjustment | identifiers in expressions, parameters (A.7.1, A.10.1) |
| pointee, element, return type  | `*`, `[ ]`, calls                              |

Types are immutable once built. `float`, `double`, `long`, `short` and `unsigned` exist
in the model from stage 1, even if programs cannot use them yet, so that no later stage
changes the representation (C6).

## 7. Implicit declarations (D25)

A call to an undeclared identifier creates a function entry at file scope with return
type int and storage class extern, marked implicit. If a later explicit declaration or
definition of the same name appears in the same file with another type, it is an error.
If it is compatible (a function returning int), the entry is updated: implicit becomes false.

## 8. Operations the table offers (names, not code)

| Operation            | Meaning                                                        |
|----------------------|----------------------------------------------------------------|
| enter scope / leave scope | push or pop a scope                                       |
| declare              | add an entry in the current scope and class; apply the redeclaration rules |
| look up              | search from the innermost scope outward in a class             |
| look up in current scope | used to detect redeclarations                              |
| is typedef name      | the parser's question; true if the nearest entry is a typedef  |
| declare member / look up member | class 2 operations with the global member rule (D24) |
| complete a tag       | attach the member list when `struct tag { ... }` is seen       |
| resolve labels       | at the end of a function, check every `goto` has a label       |

## 9. What the table does not do

- It does not decide types of expressions; semantic analysis does.
- It does not allocate registers or stack slots; code generation does, using the fields
  above.
- It does not read source text; the lexer and parser feed it.

## 10. Open items

| Item                                         | Decided when                      |
|----------------------------------------------|-----------------------------------|
| exact set of error messages for redeclarations | with the semantic phase          |
| layout fields for code generation            | when the target is chosen (D6)    |
| whether to keep scopes after leaving them (for tools) | not needed for the compiler |
