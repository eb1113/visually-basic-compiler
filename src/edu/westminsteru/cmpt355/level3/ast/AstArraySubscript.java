package edu.westminsteru.cmpt355.level3.ast;

/**
 * Array subscript expression: array[index]
 * Can be used as both lvalue and rvalue
 * @param array The array expression.
 * @param index The index expression.
 */
public record AstArraySubscript(AstExpression array, AstExpression index)
        implements AstExpression, AstLvalue {
    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(array, index);
    }
}

