# Compiler

A hand-written compiler in Java, built one phase at a time.

## Status

| Phase                | Package           | Status      |
|----------------------|-------------------|-------------|
| Lexical analysis     | `src/lexer`       | In progress |
| Parsing              | `src/parser`      | Not started |
| AST                  | `src/ast`         | Not started |
| Semantic analysis    | `src/semantic`    | Not started |
| Symbol table         | `src/symbol`      | Not started |
| Intermediate code    | `src/ir`          | Not started |
| Optimization         | `src/optimizer`   | Not started |
| Code generation      | `src/codegen`     | Not started |

## Layout

```
src/        source code, one package per compiler phase
tests/      tests
examples/   sample source programs for the language
docs/       grammar, design notes
```

## Build and run

Requires JDK 17 or newer (the lexer uses text blocks and arrow-style switch).

```
javac -d out $(find src -name "*.java")
java -cp out lexer.Lexer
```
