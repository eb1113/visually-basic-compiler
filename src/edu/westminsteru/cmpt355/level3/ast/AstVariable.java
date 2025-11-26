package edu.westminsteru.cmpt355.level3.ast;

/**
 * Represents a variable in the AST.
 * @param variable The name of the variable.
 */
public record AstVariable(String variable)
    implements AstExpression, AstLvalue {

    @Override
    public Iterable<? extends AstExpression> children() {return java.util.List.of();}
}
