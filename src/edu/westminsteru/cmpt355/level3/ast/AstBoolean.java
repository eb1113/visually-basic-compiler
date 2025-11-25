package edu.westminsteru.cmpt355.level3.ast;
/**
 * Represents a boolean literal in the AST.
 * @param value The boolean value.
 */
public record AstBoolean(boolean value) implements AstExpression {
    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of();
    }
}

