package edu.westminsteru.cmpt355.level3.ast;

/**
 * Represents a variable assignment in the AST.
 * @param lvalue The left-hand side value.
 * @param expr The right-hand side expression.
 */
public record AstVariableAssignment(AstExpression lvalue, AstExpression expr)
    implements AstStatement{

    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(lvalue, expr);
    }
}
