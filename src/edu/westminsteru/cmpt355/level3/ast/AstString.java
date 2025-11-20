package edu.westminsteru.cmpt355.level3.ast;

public record AstString(String text)
    implements AstExpression, AstPrintArgument{

    @Override
    public Iterable<? extends AstExpression> children() {return java.util.List.of();}
}
