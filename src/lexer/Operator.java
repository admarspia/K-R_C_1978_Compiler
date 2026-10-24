package lexer;

public enum Operator {
    PLUS("+"),
    MINUS("-"),
    TIMES("*"),
    DIVIDE("/"),

    ASSIGN("="),
    EQUAL_TO("=="),
    NOT_EQUAL_TO("!="),
    NOT("!"),

    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),

    SHIFT_LEFT("<<"),
    SHIFT_RIGHT(">>"),
    INCREMENT("++"),
    DECREMENT("--");

    private final String lexeme;

    Operator(String lexeme) {
        this.lexeme = lexeme;
    }

    public String getLexeme() {
        return lexeme;
    }
}
