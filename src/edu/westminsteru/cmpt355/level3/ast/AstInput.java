package edu.westminsteru.cmpt355.level3.ast;

/**
 * Represents an input statement in the AST.
 * @param prompt The prompt expression.
 */
public record AstInput(AstExpression prompt) implements AstExpression {

    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(prompt);
    }
}
