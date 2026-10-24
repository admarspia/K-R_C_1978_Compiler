package lexer;

public class IntegerToken extends Token {
    private final int value;

    public IntegerToken(String lexeme, int line, int column) {
        super(Tag.INTEGER_LITERAL, lexeme, line, column);
        this.value = Integer.parseInt(lexeme);
    }

    public int getValue() {
        return value;
    }
}
