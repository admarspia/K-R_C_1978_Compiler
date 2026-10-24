package lexer;

public class OperatorToken extends Token {
    private final Operator operator;

    public OperatorToken(Operator operator, int line, int column) {
        super(Tag.OPERATOR, operator.getLexeme(), line, column);
        this.operator = operator;
    }

    public Operator getOperator() {
        return operator;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
