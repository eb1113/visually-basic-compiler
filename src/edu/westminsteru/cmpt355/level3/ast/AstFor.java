package edu.westminsteru.cmpt355.level3.ast;

import java.util.Optional;

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

