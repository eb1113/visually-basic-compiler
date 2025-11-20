package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;

public record IrBoolean(boolean value) implements IrExpression {
    @Override
    public Type type() {
        return Type.BOOLEAN;
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of();
    }
}

