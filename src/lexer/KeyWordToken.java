package lexer;

public final class KeyWordToken extends Token {
  public KeyWordToken(String lexeme, int line, int column){
    super(Tag.KEYWORD, lexeme, line, column);
  }

}
