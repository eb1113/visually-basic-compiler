package edu.westminsteru.cmpt355.level3.ast;

public record AstInt(int value) implements AstExpression {

    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(); //empty list
    }
}
