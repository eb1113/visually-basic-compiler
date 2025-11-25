package edu.westminsteru.cmpt355.level3.ast;
/**
 * Represents an increment operation in the AST.
 * @param op The operation to perform.
 * @param lvalue The left-hand side value.
 * @param isPrefix Whether the operation is prefix or postfix.
 */
public record AstIncrement(IncrementOp op, AstExpression lvalue, boolean isPrefix)
        implements AstExpression, AstStatement {
    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(lvalue);
    }
}

