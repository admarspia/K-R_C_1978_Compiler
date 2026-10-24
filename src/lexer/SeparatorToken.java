package lexer;

public class SeparatorToken extends Token {
    private final Separator separator;

    public SeparatorToken(Separator separator, int line, int column) {
        super(
            separator == Separator.OPEN_BRACE ? Tag.BLOCK_START
                : separator == Separator.CLOSE_BRACE ? Tag.BLOCK_END
                : Tag.SEPARATOR,
            separator.getLexeme(),
            line,
            column
        );

        this.separator = separator;
    }

    public Separator getSeparator() {
        return separator;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
