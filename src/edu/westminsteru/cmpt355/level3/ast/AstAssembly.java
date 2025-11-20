package edu.westminsteru.cmpt355.level3.ast;

public record AstAssembly(String comment)
    implements AstStatement{

    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of();
    }
}
