package edu.westminsteru.cmpt355.level3.ast;

import edu.westminsteru.cmpt355.level3.lang.Type;

import java.util.List;

/**
 * Represents a parameter in the AST.
 * @param type The type of the parameter.
 * @param name The name of the parameter.
 */
public record AstParameter(Type type, String name) implements AstNode
{
    @Override
    public Iterable<? extends AstNode> children() {
        return List.of();
    }
}
