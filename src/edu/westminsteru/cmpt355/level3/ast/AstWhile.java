package edu.westminsteru.cmpt355.level3.ast;

public record AstWhile(AstExpression condition, AstBlock body)
        implements AstStatement {
    @Override
    public Iterable<? extends AstNode> children() {
        return java.util.List.of(condition, body);
    }
}

