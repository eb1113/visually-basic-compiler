package edu.westminsteru.cmpt355.level3.ast;

import java.util.Optional;

/**
 * Represents an if statement in the AST.
 * @param condition The condition expression.
 * @param thenBody The body of the then statement.
 * @param elseBody The body of the else statement.
 */
public record AstIf(AstExpression condition, AstBlock thenBody, Optional<AstBlock> elseBody)
        implements AstStatement {
    @Override
    public Iterable<? extends AstNode> children() {
        if (elseBody.isPresent()) {
            return java.util.List.of(condition, thenBody, elseBody.get());
        }
        return java.util.List.of(condition, thenBody);
    }
}

