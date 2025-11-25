package edu.westminsteru.cmpt355.level3.ast;

/**
 * Represents a while loop in the AST.
 * @param condition The condition expression.
 * @param body The body of the loop.
 */
public record AstWhile(AstExpression condition, AstBlock body)
        implements AstStatement {
    @Override
    public Iterable<? extends AstNode> children() {
        return java.util.List.of(condition, body);
    }
}

