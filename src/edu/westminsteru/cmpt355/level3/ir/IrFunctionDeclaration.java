package edu.westminsteru.cmpt355.level3.ir;
import edu.westminsteru.cmpt355.level3.lang.ReturnType;

import java.util.ArrayList;
import java.util.List;

public record IrFunctionDeclaration(
        ReturnType type,
        String name,
        List<IrParameter> parameters,
        IrBlock body
) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() {
        List<IrNode> children = new ArrayList<>(parameters);
        children.add(body);
        return children;
    }
}
