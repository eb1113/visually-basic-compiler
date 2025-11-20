package edu.westminsteru.cmpt355.level3.ast;

import edu.westminsteru.cmpt355.level3.lang.ReturnType;

import java.util.ArrayList;
import java.util.List;

public record AstFunctionDeclaration(
        ReturnType type,
        String name,
        List<AstParameter> parameters,
        AstBlock body)
        implements AstStatement {

    @Override
    public Iterable<? extends AstNode> children() {
        List<AstNode> children = new ArrayList<>(parameters);
        if (body != null) children.add(body);
        return children;
    }
}
