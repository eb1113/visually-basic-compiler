package edu.westminsteru.cmpt355.level3.ast;

import java.util.List;
import java.util.Optional;

public record AstReturn(Optional<AstExpression> expr)
    implements AstStatement{
    @Override
    public Iterable<? extends AstNode> children() {
        return expr.map(List::of).orElse(List.of());
    }
}
