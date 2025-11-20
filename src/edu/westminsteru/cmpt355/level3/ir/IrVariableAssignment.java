package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Variable;

public record IrVariableAssignment(Variable variable, IrExpression value) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(value);
    }
}