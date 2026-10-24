package lexer;

public abstract class Token {
    public final Tag tag;
    public final String lexeme;
    public final int line;
    public final int column;

    public Token(Tag tag, String lexeme, int line, int column) {
        this.tag = tag;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    public Tag getTag() { return tag; }
    public String getLexeme() { return lexeme; }
    public int getLine() { return line; }
    public int getColumn() { return column; }

    @Override
    public String toString() {
        return tag + "('" + lexeme + "') at " + line + ":" + column;
    }
}
