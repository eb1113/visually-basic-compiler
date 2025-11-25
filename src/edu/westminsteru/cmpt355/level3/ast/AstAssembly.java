package edu.westminsteru.cmpt355.level3.ast;
/**
 * Represents an assembly comment in the AST.
 * @param comment The comment text.
 */
public record AstAssembly(String comment)
    implements AstStatement{

    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of();
    }
}
