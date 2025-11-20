package edu.westminsteru.cmpt355.level3.ast;

public record AstVariable(String variable)
    implements AstExpression {

    @Override
    public Iterable<? extends AstExpression> children() {return java.util.List.of();}
}
