package edu.westminsteru.cmpt355.level3.ast;

public record AstBinary(BinaryOp op, AstExpression left, AstExpression right)
    implements AstExpression
{
    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(left, right);
    }
}

