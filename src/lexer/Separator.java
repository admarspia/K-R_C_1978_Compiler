package lexer;

public enum Separator {
    OPEN_PAREN("("),
    CLOSE_PAREN(")"),
    SEMICOLON(";"),
    COMMA(","),
    OPEN_BRACE("{"),
    CLOSE_BRACE("}");

    private final String lexeme;

    Separator(String lexeme) {
        this.lexeme = lexeme;
    }

    public String getLexeme() {
        return lexeme;
    }
}

