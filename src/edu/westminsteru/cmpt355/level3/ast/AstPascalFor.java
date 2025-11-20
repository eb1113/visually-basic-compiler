package edu.westminsteru.cmpt355.level3.ast;

import edu.westminsteru.cmpt355.level3.lang.Type;

public record AstPascalFor(Type type, String varName, AstExpression startVal, AstExpression endVal, AstExpression stepVal, AstBlock body)
        implements AstStatement {
    @Override
    public Iterable<? extends AstNode> children() {
        return java.util.List.of(startVal, endVal, stepVal, body);
    }
}

