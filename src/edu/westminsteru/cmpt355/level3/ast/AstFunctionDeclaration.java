package edu.westminsteru.cmpt355.level3.ast;

import edu.westminsteru.cmpt355.level3.lang.ReturnType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a function declaration in the AST.
 * @param type The return type of the function.
 * @param name The name of the function.
 * @param parameters The parameters of the function.
 * @param body The body of the function.
 */
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
