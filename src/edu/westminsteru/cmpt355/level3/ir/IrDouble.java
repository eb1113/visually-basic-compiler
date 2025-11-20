package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;

public record IrDouble(double value) implements IrExpression {
    @Override
    public Type type() {
        return Type.DOUBLE;
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of();
    }
}
