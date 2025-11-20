package edu.westminsteru.cmpt355.level3.ast;

import java.util.List;

/** AST node type representing the program as a whole: the root AST node */
public record AstProgram(List<AstStatement> code) implements AstNode {

    @Override
    public Iterable<? extends AstNode> children() {
        return code;
    }

    @Override
    public String toString() {
        return "Program";
    }
}
