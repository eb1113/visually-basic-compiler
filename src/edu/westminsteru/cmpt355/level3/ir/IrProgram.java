package edu.westminsteru.cmpt355.level3.ir;

import java.util.ArrayList;
import java.util.List;

/** IR node type representing the program as a whole: the root IR node */
public record IrProgram(List<IrFunctionDeclaration> functions, List<IrStatement> statements) implements IrNode {

    @Override
    public Iterable<? extends IrNode> children() {
        List<IrNode> children = new ArrayList<>();
        children.addAll(functions);
        children.addAll(statements);
        return children;
    }

    @Override
    public String toString() {
        return "Program";
    }
}
