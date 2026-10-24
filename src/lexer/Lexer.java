package lexer;

import java.io.*;
import java.util.*;

public class Lexer {
  private static final String SOURCE = """
    int main() {
      int x = 10;
      float price = 25.50;
      x++;
      x--;
      if (x >= 10 ) {
        x = x + 1;
      }
      // This is a single-line comment
      return x;
    }

    """;

  private static final Set<String> KEYWORDS = Set.of(
      "true", "false", "if", "else", "while",
      "for", "int", "float", "double", "char"
      );

  private int line = 1;
  private int column = 1;
  private char peek = ' ';
  private int lookahead;
  private final PushbackReader pr;

  // LinkedHashMap keeps insertion order, so the table prints in scan order.
  private final Map<String, Token> symbolTable = new LinkedHashMap<>();

  public Lexer(String source) {
    this.pr = new PushbackReader(new StringReader(source), 2);
  }

  public static void main(String[] args) {
    Lexer lexer = new Lexer(SOURCE);
    lexer.lexer();
    lexer.showTable();
  }

  public void lexer() {
    Token t;

    while ((t = scan()) != null) {
      symbolTable.put(line + ":" + column + ":" + symbolTable.size(), t);
    }
  }

  public void showTable() {
    System.out.printf("%-20s %-20s%n", "TOKEN TYPE", "LEXEME");
    System.out.println("----------------------------------------");

    for (Token t : symbolTable.values()) {
      System.out.printf("%-20s %-20s%n", t.tag, t.lexeme);
    }
  }

  public Token scan() {
    try {
      int ch;

      // Skip whitespace and comments.
      while (true) {
        ch = pr.read();

        if (ch == -1) return null;

        peek = (char) ch;

        if (peek == ' ' || peek == '\t' || peek == '\r') {
          column++;
          continue;
        }

        if (peek == '\n') {
          line++;
          column = 1;
          continue;
        }

        if (peek == '/') {
          lookahead = pr.read();

          if (lookahead == '/') {
            column += 2;

            while ((ch = pr.read()) != -1 && ch != '\n') column++;

            if (ch == '\n') {
              line++;
              column = 1;
            }

            continue;
          } else if (lookahead == '*') {
            column += 2;
            boolean closed = false;
            int previous = -1;

            while ((ch = pr.read()) != -1) {
              if (ch == '\n') {
                line++;
                column = 1;
              } else {
                column++;
              }

              if (previous == '*' && ch == '/') {
                closed = true;
                break;
              }

              previous = ch;
            }

            if (!closed) {
              throw new IOException("Unterminated block comment at " + line + ":" + column);
            }

            continue;
          } else {
            if (lookahead != -1) pr.unread(lookahead);

            int divLine = line;
            int divColumn = column++;
            return new OperatorToken(Operator.DIVIDE, divLine, divColumn);
          }
        }

        break;
      }

      // The first character of this token is already consumed.
      int startLine = line;
      int startColumn = column;

      if (!Character.isLetterOrDigit(peek) && peek != '_') {
        switch (peek) {
          case '+' -> {
            lookahead = pr.read();

            if (lookahead == '+') {
              column += 2;
              return new OperatorToken(Operator.INCREMENT, startLine, startColumn);
            }

            if (lookahead != -1) pr.unread(lookahead);

            column++;
            return new OperatorToken(Operator.PLUS, startLine, startColumn);
          }

          case '-' -> {
            lookahead = pr.read();

            if (lookahead == '-') {
              column += 2;
              return new OperatorToken(Operator.DECREMENT, startLine, startColumn);
            }

            if (lookahead != -1) pr.unread(lookahead);

            column++;
            return new OperatorToken(Operator.MINUS, startLine, startColumn);
          }

          case '*' -> {
            column++;
            return new OperatorToken(Operator.TIMES, startLine, startColumn);
          }

          case '=' -> {
            lookahead = pr.read();

            if (lookahead == '=') {
              column += 2;
              return new OperatorToken(Operator.EQUAL_TO, startLine, startColumn);
            }

            if (lookahead != -1) pr.unread(lookahead);

            column++;
            return new OperatorToken(Operator.ASSIGN, startLine, startColumn);
          }

          case '!' -> {
            lookahead = pr.read();

            if (lookahead == '=') {
              column += 2;
              return new OperatorToken(Operator.NOT_EQUAL_TO, startLine, startColumn);
            }

            if (lookahead != -1) pr.unread(lookahead);

            column++;
            return new OperatorToken(Operator.NOT, startLine, startColumn);
          }

          case '>' -> {
            lookahead = pr.read();

            if (lookahead == '=') {
              column += 2;
              return new OperatorToken(Operator.GREATER_THAN_OR_EQUAL, startLine, startColumn);
            } else if (lookahead == '>') {
              column += 2;
              return new OperatorToken(Operator.SHIFT_RIGHT, startLine, startColumn);
            }

            if (lookahead != -1) pr.unread(lookahead);

            column++;
            return new OperatorToken(Operator.GREATER_THAN, startLine, startColumn);
          }

          case '<' -> {
            lookahead = pr.read();

            if (lookahead == '=') {
              column += 2;
              return new OperatorToken(Operator.LESS_THAN_OR_EQUAL, startLine, startColumn);
            } else if (lookahead == '<') {
              column += 2;
              return new OperatorToken(Operator.SHIFT_LEFT, startLine, startColumn);
            }

            if (lookahead != -1) pr.unread(lookahead);

            column++;
            return new OperatorToken(Operator.LESS_THAN, startLine, startColumn);
          }

          case '(' -> {
            column++;
            return new SeparatorToken(Separator.OPEN_PAREN, startLine, startColumn);
          }

          case ')' -> {
            column++;
            return new SeparatorToken(Separator.CLOSE_PAREN, startLine, startColumn);
          }

          case ';' -> {
            column++;
            return new SeparatorToken(Separator.SEMICOLON, startLine, startColumn);
          }

          case ',' -> {
            column++;
            return new SeparatorToken(Separator.COMMA, startLine, startColumn);
          }

          case '{' -> {
            column++;
            return new SeparatorToken(Separator.OPEN_BRACE, startLine, startColumn);
          }

          case '}' -> {
            column++;
            return new SeparatorToken(Separator.CLOSE_BRACE, startLine, startColumn);
          }

          default -> throw new IOException(
              "Unrecognized character '" + peek + "' at " + startLine + ":" + startColumn
              );
        }
      }

      // Integer and floating-point literals.
      if (Character.isDigit(peek)) {
        StringBuilder b = new StringBuilder();
        boolean isFloat = false;

        b.append(peek);
        column++;

        while ((ch = pr.read()) != -1) {
          char c = (char) ch;

          if (Character.isDigit(c)) {
            b.append(c);
            column++;
          } else if (c == '.' && !isFloat) {
            isFloat = true;
            b.append(c);
            column++;
          } else {
            pr.unread(ch);
            break;
          }
        }

        if (b.charAt(b.length() - 1) == '.') {
          throw new IOException("Invalid floating-point literal: " + b);
        }

        if (isFloat) return new FloatToken(b.toString(), startLine, startColumn);

        return new IntegerToken(b.toString(), startLine, startColumn);
      }

      // Identifiers and keywords.
      StringBuilder b = new StringBuilder();
      b.append(peek);
      column++;

      while ((ch = pr.read()) != -1) {
        char c = (char) ch;

        if (Character.isLetterOrDigit(c) || c == '_') {
          b.append(c);
          column++;
        } else {
          pr.unread(ch);
          break;
        }
      }

      String lexeme = b.toString();

      if (KEYWORDS.contains(lexeme)) {
        return new KeyWordToken(lexeme, startLine, startColumn);
      }

      return new IdentifierToken(lexeme, startLine, startColumn);

    } catch (IOException e) {
      throw new RuntimeException("Lexer error: " + e.getMessage(), e);
    }
  }
}
