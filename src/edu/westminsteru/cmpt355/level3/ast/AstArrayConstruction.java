package edu.westminsteru.cmpt355.level3.ast;

import edu.westminsteru.cmpt355.level3.lang.ScalarType;

import java.util.List;

/**
 * Array construction: new int[100] or new int[10][20]...
 * @param baseType The base type of the array (ScalarType before adding dimensions).
 * @param dimensions List of dimension size expressions.
 */
public record AstArrayConstruction(ScalarType baseType, List<AstExpression> dimensions)
        implements AstExpression {
    @Override
    public Iterable<? extends AstExpression> children() {
        return dimensions;
    }
}

