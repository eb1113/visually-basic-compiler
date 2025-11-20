package edu.westminsteru.cmpt355.level3.ast;

public record AstVariableAssignment(String variable, AstExpression expr)
    implements AstStatement{

    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(expr);
    }
}
