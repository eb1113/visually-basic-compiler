package edu.westminsteru.cmpt355.level3.ast;

/**
 * Represents a binary operation in the AST.
 * @param op The binary operation.
 * @param left The left operand.
 * @param right The right operand.
 */
public record AstBinary(BinaryOp op, AstExpression left, AstExpression right)
    implements AstExpression
{
    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(left, right);
    }
}

