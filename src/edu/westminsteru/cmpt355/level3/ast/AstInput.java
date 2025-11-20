package edu.westminsteru.cmpt355.level3.ast;

public record AstInput(AstExpression prompt) implements AstExpression {

    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(prompt);
    }
}
