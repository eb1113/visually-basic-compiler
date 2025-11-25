package edu.westminsteru.cmpt355.level3.ast;

import java.util.List;
import java.util.Optional;

/**
 * Represents a return statement in the AST.
 * @param expr The expression to return.
 */
public record AstReturn(Optional<AstExpression> expr)
    implements AstStatement{
    @Override
    public Iterable<? extends AstNode> children() {
        return expr.map(List::of).orElse(List.of());
    }
}
