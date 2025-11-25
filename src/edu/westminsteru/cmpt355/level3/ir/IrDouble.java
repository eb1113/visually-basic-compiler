package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.BuiltInType;

public record IrDouble(double value) implements IrExpression {
    @Override
    public BuiltInType type() {
        return BuiltInType.DOUBLE;
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of();
    }
}
