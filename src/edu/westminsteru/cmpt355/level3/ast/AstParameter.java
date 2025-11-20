package edu.westminsteru.cmpt355.level3.ast;

import edu.westminsteru.cmpt355.level3.lang.Type;

import java.util.List;

public record AstParameter(Type type, String name) implements AstNode
{
    @Override
    public Iterable<? extends AstNode> children() {
        return List.of();
    }
}
