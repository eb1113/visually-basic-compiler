package edu.westminsteru.cmpt355.level3.ast;

import java.util.List;

/** AST node representing the program as a whole: the root AST node */
public record AstProgram(List<AstFunctionDeclaration> funcs, List<AstRecordDeclaration> records, List<AstStatement> stmts) implements AstNode {

    @Override
    public Iterable<? extends AstNode> children() {
        List<AstNode> allChildren = new java.util.ArrayList<>();
        allChildren.addAll(funcs);
        allChildren.addAll(records);
        allChildren.addAll(stmts);
        return allChildren;
    }

    @Override
    public String toString() {
        return "Program";
    }
}
