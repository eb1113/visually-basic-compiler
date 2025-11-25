package edu.westminsteru.cmpt355.level3.ast;

import edu.westminsteru.cmpt355.level3.lang.Type;

/**
 * Array construction: new int[100]
 * @param elementType The type of the elements in the array.
 * @param size The size of the array.
 */
public record AstArrayConstruction(Type elementType, AstExpression size)
        implements AstExpression {
    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(size);
    }
}

