package lexer;

public class FloatToken extends Token {
    private final float value;

    public FloatToken(String lexeme, int line, int column) {
        super(Tag.FLOAT_LITERAL, lexeme, line, column);
        this.value = Float.parseFloat(lexeme);
    }

    public float getValue() {
        return value;
    }
}
