package edu.westminsteru.cmpt355.level3.ast;

import edu.westminsteru.cmpt355.level3.lang.Type;

import java.util.List;
import java.util.Optional;

public record AstVariableDeclaration(Type type, String name, Optional<AstExpression> expr)
        implements AstStatement{

    @Override
    public Iterable<? extends AstExpression> children() {
        return expr.map(List::of).orElse(List.of());
    }
}
