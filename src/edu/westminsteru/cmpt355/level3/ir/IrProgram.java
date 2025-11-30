package edu.westminsteru.cmpt355.level3.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** IR node type representing the program as a whole: the root IR node */
public record IrProgram(
    List<IrFunctionDeclaration> functions, 
    List<IrStatement> statements,
    Map<String, List<IrFunctionDeclaration>> recordMethods
) implements IrNode {

    @Override
    public Iterable<? extends IrNode> children() {
        List<IrNode> children = new ArrayList<>();
        children.addAll(functions);
        children.addAll(statements);
        recordMethods.values().forEach(methods -> children.addAll(methods));
        return children;
    }

    @Override
    public String toString() {
        return "Program";
    }
}
