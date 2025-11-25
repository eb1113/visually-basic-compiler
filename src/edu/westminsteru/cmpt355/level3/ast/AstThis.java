package edu.westminsteru.cmpt355.level3.ast;

/**
 * 'this' expression - refers to the current record instance
 * Only valid inside record methods
 */
public record AstThis() implements AstExpression {
    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of();
    }
}

