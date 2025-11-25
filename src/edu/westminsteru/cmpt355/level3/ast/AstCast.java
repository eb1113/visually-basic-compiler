package edu.westminsteru.cmpt355.level3.ast;

import edu.westminsteru.cmpt355.level3.lang.Type;
/**
 * Represents a cast operation in the AST.
 * @param targetType The target type of the cast.
 * @param expr The expression to be cast.
 */
public record AstCast(Type targetType, AstExpression expr) implements AstExpression {
    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(expr);
    }
}

