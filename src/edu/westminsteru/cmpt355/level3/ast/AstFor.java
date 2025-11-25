package edu.westminsteru.cmpt355.level3.ast;

import java.util.Optional;

/**
 * Represents a for loop in the AST.
 * @param init The initialization statement.
 * @param condition The condition expression.
 * @param update The update statement.
 * @param body The body of the loop.
 */
public record AstFor(Optional<AstStatement> init, Optional<AstExpression> condition, Optional<AstStatement> update, AstBlock body)
        implements AstStatement {
    @Override
    public Iterable<? extends AstNode> children() {
        java.util.List<AstNode> children = new java.util.ArrayList<>();
        init.ifPresent(children::add);
        condition.ifPresent(children::add);
        update.ifPresent(children::add);
        children.add(body);
        return children;
    }
}

