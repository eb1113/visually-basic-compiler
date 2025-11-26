package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;
import edu.westminsteru.cmpt355.level3.lang.Variable;

public record IrVariable(Variable variable) implements IrExpression, IrLvalue {
    @Override
    public Type type() {
        return variable.type();
    }
    
    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of();
    }
}
