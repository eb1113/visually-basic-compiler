package edu.westminsteru.cmpt355.level3.ast;

import java.util.List;

/**
 * Represents a function call in the AST.
 * @param name The name of the function.
 * @param args The arguments to the function.
 */
public record AstFunctionCall(String name, List<AstExpression> args)
    implements AstStatement, AstExpression{
    @Override
    public Iterable<? extends AstNode> children() {
        return args;
    }
}
