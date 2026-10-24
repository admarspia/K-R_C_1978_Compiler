package lexer;

public class IdentifierToken extends Token {
    public IdentifierToken(String lexeme, int line, int column) {
        super(Tag.IDENTIFIER, lexeme, line, column);
    }
}
