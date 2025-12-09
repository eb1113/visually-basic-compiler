package edu.westminsteru.cmpt355.level3.ast;

/**
 * Represents a unary operation in the AST.
 * @param op The unary operation.
 * @param expr The expression to be operated on.
 */
public record AstUnary(UnaryOp op, AstExpression expr) implements AstExpression {
    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(expr);
    }
}

