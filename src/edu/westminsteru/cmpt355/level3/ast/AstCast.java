package edu.westminsteru.cmpt355.level3.ast;

import edu.westminsteru.cmpt355.level3.lang.Type;

public record AstCast(Type targetType, AstExpression expr) implements AstExpression {
    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(expr);
    }
}

