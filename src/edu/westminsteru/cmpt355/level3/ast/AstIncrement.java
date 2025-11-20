package edu.westminsteru.cmpt355.level3.ast;

public record AstIncrement(IncrementOp op, AstVariable variable, boolean isPrefix)
        implements AstExpression, AstStatement {
    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(variable);
    }
}

