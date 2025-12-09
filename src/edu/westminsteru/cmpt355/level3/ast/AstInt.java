package edu.westminsteru.cmpt355.level3.ast;

/**
 * Represents an integer literal in the AST.
 * @param value The integer value.
 */
public record AstInt(int value) implements AstExpression {

    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(); //empty list
    }
}
