package edu.westminsteru.cmpt355.level3.ast;

import java.util.List;

public record AstFunctionCall(String name, List<AstExpression> args)
    implements AstStatement, AstExpression{
    @Override
    public Iterable<? extends AstNode> children() {
        return args;
    }
}
