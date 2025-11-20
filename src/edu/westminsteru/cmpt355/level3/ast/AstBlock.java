package edu.westminsteru.cmpt355.level3.ast;

import java.util.List;

public record AstBlock(List<AstStatement> statements)
        implements AstStatement {
    @Override
    public Iterable<? extends AstNode> children() {
        return statements;
    }
}
